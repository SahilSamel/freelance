package com.freelance.event.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventMessage;
import com.freelance.rest.app.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * <p>
 * Message dispatching, priority ordering, and delayed retry re-publishing are managed by the active
 * {@link com.freelance.event.broker.EventBroker} driver implementation.
 * </p>
 * <p>
 * <pre>{@code
 *                                [ Producer: emit(destination, payload) ]
 *                                                   │
 *                                                   ▼
 *                                ┌──────────────────────────────────────┐
 *    ┌──────────────────────────►│  EventBroker Priority Queue / ZSET   │
 *    │                           │  (Ordered: Priority -> FIFO Seq)     │
 *    │                           └──────────────────┬───────────────────┘
 *    │                                              │
 *    │                                              ▼ (Worker Poll & Rate Limiter)
 *    │                           ┌──────────────────────────────────────┐
 *    │                           │     GenericConsumerWorker.run()      │
 *    │                           │  - Set MDC traceId                   │
 *    │                           │  - Deserialize target DTO payload    │
 *    │                           │  - Start JPA Tenant Transaction      │
 *    │                           │  - Bind caller RequestContext userId │
 *    │                           └──────────────────┬───────────────────┘
 *    │                                              │
 *    │                                              ▼
 *    │                                  [ handler.handle(msg) ]
 *    │                                              │
 *    │                    ┌─────────────────────────┴─────────────────────────┐
 *    │                    │                                                   │
 *    │               (Success)                                            (Exception)
 *    │                    │                                                   │
 *    │                    ▼                                                   ▼
 *    │        ┌───────────────────────┐                           ┌───────────────────────┐
 *    │        │ Transaction.finish()  │                           │ Rollback Transaction  │
 *    │        │ [Task Succeeded]      │                           └───────────┬───────────┘
 *    │        └───────────────────────┘                                       │
 *    │                                                                        ▼
 *    │                                                              [ handleFailure() ]
 *    │                                                                        │
 *    │                                      ┌─────────────────────────────────┴─────────────────────────────────┐
 *    │                                      │                                                                   │
 *    │                           (retryCount < maxRetries)                                          (retryCount >= maxRetries)
 *    │                                      │                                                                   │
 *    │                                      ▼                                                                   ▼
 *    │                      ┌───────────────────────────────┐                                   ┌───────────────────────────────┐
 *    │                      │ Calculate Exponential Backoff │                                   │ Open Isolated JPA Transaction │
 *    │                      │ retryCount++                  │                                   │ handler.onDeadLetter()        │
 *    │                      │ broker.publishDelayed(delay)  │                                   │ DLQ Transaction.finish()      │
 *    │                      └───────────────┬───────────────┘                                   └───────────────────────────────┘
 *    │                                      │                                                                   │
 *    │                                      ▼ (Delay timer expires)                                             ▼
 *    │                          ┌───────────────────────┐                                               [ Permanent DLQ ]
 *    └──────────────────────────┤ Re-enqueued to Queue  │
 *                               └───────────────────────┘
 * }</pre>
 * </p>
 *
 * @param <T> the payload type
 * @see com.freelance.event.broker.EventBroker
 */
public class GenericConsumerWorker<T> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GenericConsumerWorker.class);
    private static final String MDC_TRACE_ID = "traceId";

    private final EventMessage<?> rawMessage;
    private final Class<T> payloadClass;
    private final EventHandler<T> handler;
    private final ConsumerConfig config;
    private final EventBroker broker;
    private final ObjectMapper jsonMapper;

    public GenericConsumerWorker(
            EventMessage<?> rawMessage,
            Class<T> payloadClass,
            EventHandler<T> handler,
            ConsumerConfig config,
            EventBroker broker
    ) {
        this.rawMessage = rawMessage;
        this.payloadClass = payloadClass;
        this.handler = handler;
        this.config = config;
        this.broker = broker;
        this.jsonMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void run() {
        String traceId = rawMessage.getTraceId();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_TRACE_ID, traceId);

        EventMessage<T> typedMessage = null;
        boolean txStarted = false;

        try {
            typedMessage = convertPayload(rawMessage, payloadClass);

            try {
                Transaction.start(rawMessage.getTenantId());
                txStarted = true;
            } catch (Exception e) {
                logger.debug("Transaction engine not initialized for event worker: {}", e.getMessage());
            }

            if (txStarted && typedMessage.getCallerUserId() != null) {
                Transaction.getRequestContext().withUserId(typedMessage.getCallerUserId());
            }

            handler.handle(typedMessage);

            if (txStarted) {
                Transaction.finish();
            }

        } catch (Throwable ex) {
            if (txStarted) {
                try {
                    Transaction.peek().markRollbackOnly();
                    Transaction.finish();
                } catch (Exception rollbackEx) {
                    logger.debug("Rollback completed or suppressed: {}", rollbackEx.getMessage());
                }
            }

            handleFailure(typedMessage != null ? typedMessage : rawMessage, ex);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    @SuppressWarnings("unchecked")
    private EventMessage<T> convertPayload(EventMessage<?> msg, Class<T> targetClass) {
        Object payload = msg.getPayload();
        if (payload == null || targetClass == null || targetClass.equals(Object.class) || targetClass.isInstance(payload)) {
            return (EventMessage<T>) msg;
        }

        try {
            T converted = jsonMapper.convertValue(payload, targetClass);
            return msg.withPayload(converted);
        } catch (Exception e) {
            logger.warn("Payload conversion via ObjectMapper failed, using raw payload: {}", e.getMessage());
            return (EventMessage<T>) msg;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleFailure(EventMessage<?> msg, Throwable cause) {
        if (msg.getRetryCount() < config.maxRetries()) {
            int nextRetry = msg.getRetryCount() + 1;
            long delay = (long) (config.backoffMillis() * Math.pow(config.backoffMultiplier(), msg.getRetryCount()));
            logger.warn("[EventManagementEngine] Task {} on topic '{}' failed (attempt {}/{}). Retrying in {}ms. Error: {}",
                    msg.getId(), msg.getDestination(), nextRetry, config.maxRetries(), delay, cause.getMessage());

            EventMessage<?> retryMsg = msg.withRetryCount(nextRetry);
            broker.publishDelayed(retryMsg, delay);
        } else {
            logger.error("[EventManagementEngine] Task {} on topic '{}' exhausted all {} retries. Routing to Dead-Letter Handler.",
                    msg.getId(), msg.getDestination(), config.maxRetries(), cause);

            if (handler != null) {
                boolean dlqTxStarted = false;
                try {
                    try {
                        Transaction.start(msg.getTenantId());
                        dlqTxStarted = true;
                        if (msg.getCallerUserId() != null) {
                            Transaction.getRequestContext().withUserId(msg.getCallerUserId());
                        }
                    } catch (Exception txEx) {
                        logger.debug("Transaction engine not initialized for DLQ worker: {}", txEx.getMessage());
                    }

                    handler.onDeadLetter((EventMessage<T>) msg, cause);

                    if (dlqTxStarted) {
                        Transaction.finish();
                    }
                } catch (Throwable dlqEx) {
                    if (dlqTxStarted) {
                        try {
                            Transaction.peek().markRollbackOnly();
                            Transaction.finish();
                        } catch (Exception rollbackEx) {
                            logger.debug("DLQ Rollback completed or suppressed: {}", rollbackEx.getMessage());
                        }
                    }
                    logger.error("[EventManagementEngine] DeadLetterHandler failed during error handling", dlqEx);
                }
            }
        }
    }
}
