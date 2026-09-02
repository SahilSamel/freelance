package com.freelance.event.consumer;

import com.freelance.event.model.EventConsumer;
import com.freelance.event.model.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functional interface implemented by consumers to process events received from a destination topic.
 * <p>
 * Classes implementing this interface must be annotated with {@link EventConsumer}
 * for automatic classpath discovery, configuration, and topic subscription on application startup.
 * <p>
 * <b>Package Requirement:</b> All event consumers <b>MUST</b> reside within the package {@code com.freelance.consumer}
 * (or its subpackages, e.g. {@code com.freelance.consumer.auth}, {@code com.freelance.consumer.notifications}) across any module.
 * The framework scanner enforces this requirement and will fail startup if placed in any other package.
 * </p>
 * <p>
 * Execution of both {@link #handle} and {@link #onDeadLetter} takes place inside an isolated worker
 * thread with an active JPA {@code Transaction} boundary, caller {@code RequestContext}, and logging {@code MDC} traceId.
 * </p>
 *
 * @param <T> the type of the domain payload object
 * @see EventConsumer
 */
@FunctionalInterface
public interface EventHandler<T> {

    /**
     * Handles the incoming event message.
     * <p>
     * If execution throws any exception, the active transaction is automatically rolled back and retry / DLQ logic is triggered.
     * </p>
     */
    void handle(EventMessage<T> message) throws Exception;

    /**
     * Invoked when an event message permanently fails after exhausting all configured retry attempts.
     */
    default void onDeadLetter(EventMessage<T> message, Throwable cause) {
        Logger logger = LoggerFactory.getLogger(getClass());
        logger.error("================================================================================");
        logger.error("[DEAD_LETTER_QUEUE] Event message permanently failed after {} retries!",
                message.getRetryCount());
        logger.error("  Destination   : {}", message.getDestination());
        logger.error("  Message ID    : {}", message.getId());
        logger.error("  Priority      : {}", message.getPriority());
        logger.error("  Trace ID      : {}", message.getTraceId());
        logger.error("  Caller User ID: {}", message.getCallerUserId());
        logger.error("  Timestamp     : {}", message.getTimestamp());
        logger.error("  Payload Type  : {}", message.getPayloadType());
        logger.error("  Payload       : {}", message.getPayload());
        logger.error("  Terminal Cause: {}", cause != null ? cause.getMessage() : "Unknown", cause);
        logger.error("================================================================================");
    }
}
