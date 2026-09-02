package com.freelance.event.model;

import com.freelance.event.IConsumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an asynchronous event consumer for automatic classpath discovery and registration on startup.
 * <p>
 * Classes annotated with {@code @EventConsumer} must implement {@link com.freelance.event.consumer.EventHandler}.
 * </p>
 * <p>
 * <b>Package Requirement:</b> All event consumers <b>MUST</b> reside within the package {@code com.freelance.consumer}
 * (or its subpackages) across any module. The framework scanner strictly enforces this convention.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventConsumer {

    /**
     * The unique destination topic string to subscribe to, corresponding to an {@link IConsumer#getDestination()} entry.
     *
     * @return destination topic identifier
     */
    String destination();

    /**
     * The target domain DTO class to deserialize JSON payloads into (e.g. {@code UserCreatedDTO.class}).
     * <p>
     * Before invoking {@link com.freelance.event.consumer.EventHandler#handle}, the framework automatically converts
     * the message payload into this strongly-typed class. Defaults to {@link Object} for untyped or raw payloads.
     * </p>
     *
     * @return the target payload class
     */
    Class<?> payloadType() default Object.class;

    /**
     * Number of parallel background worker threads dedicated to polling and executing tasks for this topic.
     * <p>
     * Increasing this allows higher parallel throughput for slow/IO-bound operations (e.g. external network calls).
     * Defaults to {@code 1}.
     * </p>
     *
     * @return worker thread concurrency count (minimum 1)
     */
    int concurrency() default 1;

    /**
     * Maximum throughput rate limit (tasks per second) enforced across all worker threads via a Token Bucket limiter.
     * <p>
     * Prevents downstream third-party APIs (e.g. WhatsApp, Payment Gateways) from returning HTTP 429 rate limit errors.
     * Defaults to {@code 0.0} (unlimited throughput).
     * </p>
     *
     * @return maximum allowed tasks per second
     */
    double rateLimitPerSecond() default 0.0;

    /**
     * Maximum number of retry attempts for transient failures before invoking {@link com.freelance.event.consumer.EventHandler#onDeadLetter}.
     * <p>
     * Defaults to {@code 3}. Set to {@code 0} to disable retries.
     * </p>
     *
     * @return maximum retry count
     */
    int maxRetries() default 3;

    /**
     * Initial backoff delay in milliseconds before executing the first retry attempt.
     * <p>
     * Defaults to {@code 1000L} (1 second).
     * </p>
     *
     * @return initial retry backoff duration in milliseconds
     */
    long backoffMillis() default 1000L;

    /**
     * Multiplier applied to calculate exponential retry backoff delays on subsequent attempts.
     * <p>
     * For example, with {@code backoffMillis = 1000} and {@code backoffMultiplier = 2.0}, retries occur at:
     * <ul>
     *   <li>Retry 1: 1,000ms</li>
     *   <li>Retry 2: 2,000ms</li>
     *   <li>Retry 3: 4,000ms</li>
     * </ul>
     * Defaults to {@code 2.0}.
     * </p>
     *
     * @return exponential backoff multiplier factor
     */
    double backoffMultiplier() default 2.0;
}
