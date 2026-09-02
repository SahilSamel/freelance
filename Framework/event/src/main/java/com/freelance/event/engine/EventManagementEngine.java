package com.freelance.event.engine;

import com.freelance.ConfigurationEngine;
import com.freelance.event.IConsumer;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.broker.EventBrokerFactory;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.keys.event.EventConfigKeys;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class EventManagementEngine {

    private static final Logger logger = LoggerFactory.getLogger(EventManagementEngine.class);
    private static final String MDC_TRACE_ID = "traceId";

    private static final class Holder {
        private static final EventManagementEngine INSTANCE = new EventManagementEngine();
    }

    public static EventManagementEngine getInstance() {
        return Holder.INSTANCE;
    }

    private volatile EventBroker broker;
    private volatile boolean initialized = false;

    private EventManagementEngine() {
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }

        if (this.broker == null) {
            this.broker = EventBrokerFactory.createBroker();
        }

        EventConsumerScanner scanner = new EventConsumerScanner();
        scanner.scanAndRegister(this.broker);

        this.broker.start();
        this.initialized = true;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            int timeoutSec = ConfigurationEngine.getInt(EventConfigKeys.EVENT_SHUTDOWN_TIMEOUT_SECONDS);
            shutdown(timeoutSec * 1000L);
        }, "event-engine-shutdown-hook"));

        logger.info("[EventManagementEngine] Event management framework initialized successfully.");
    }

    public synchronized void shutdown(long timeoutMillis) {
        if (this.broker != null) {
            this.broker.shutdown(timeoutMillis);
        }
        this.initialized = false;
    }

    /**
     * Programmatically registers a consumer handler for a type-safe {@link IConsumer} topic.
     *
     * @param <T>          payload type
     * @param destination  topic interface/enum
     * @param config       consumer throttling and retry settings
     * @param payloadClass target class for JSON deserialization
     * @param handler      consumer functional handler
     */
    public <T> void registerConsumer(
            IConsumer destination,
            ConsumerConfig config,
            Class<T> payloadClass,
            EventHandler<T> handler
    ) {
        Objects.requireNonNull(destination, "destination must not be null");
        getOrCreateBroker().subscribe(destination, config, payloadClass, handler);
    }

    private EventBroker getOrCreateBroker() {
        if (this.broker == null) {
            synchronized (this) {
                if (this.broker == null) {
                    this.broker = EventBrokerFactory.createBroker();
                    this.broker.start();
                }
            }
        }
        return this.broker;
    }

    /**
     * Emits an event using a type-safe {@link IConsumer} destination and priority level.
     *
     * @param destination target topic interface/enum
     * @param payload     the domain event payload object
     * @param priority    execution precedence level
     * @param <T>         the payload type
     */
    public static <T> void emit(IConsumer destination, T payload, EventPriority priority) {
        Objects.requireNonNull(destination, "destination must not be null");

        UUID callerUserId = null;
        try {
            if (Transaction.getRequestContext() != null) {
                callerUserId = Transaction.getRequestContext().userId();
            }
        } catch (Exception ignored) {
            callerUserId = SystemHardcoding.SYSTEM_USER_ID.asUuid();
        }

        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            logger.debug("Added random traceId for {}", destination.getDestination());
        }

        EventMessage<T> message = new EventMessage<>(
                UUID.randomUUID(),
                destination.getDestination(),
                payload,
                payload != null ? payload.getClass().getName() : null,
                priority != null ? priority : EventPriority.NORMAL,
                callerUserId,
                traceId,
                Instant.now(),
                0,
                0L
        );

        getInstance().getOrCreateBroker().publish(message);
    }

    /**
     * Emits a standard background event using a type-safe {@link IConsumer} destination with {@link EventPriority#NORMAL} priority.
     * <p>
     * Automatically captures the caller's {@code userId} and {@code traceId} from the current request thread context.
     * </p>
     *
     * @param destination target topic interface/enum
     * @param payload     the domain event payload object
     * @param <T>         the payload type
     */
    public static <T> void emit(IConsumer destination, T payload) {
        emit(destination, payload, EventPriority.NORMAL);
    }

    /**
     * Emits a fully customized {@link EventMessage} envelope.
     *
     * @param message custom event envelope
     * @param <T>     the payload type
     */
    public static <T> void emit(EventMessage<T> message) {
        if (message == null) return;
        getInstance().getOrCreateBroker().publish(message);
    }

    /**
     * Resets the broker instance. Intended exclusively for test fixtures.
     */
    public synchronized void setBrokerForTesting(EventBroker broker) {
        if (this.broker != null && this.broker != broker) {
            this.broker.shutdown(500);
        }
        this.broker = broker;
        this.initialized = (broker != null);
    }
}
