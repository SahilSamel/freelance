package com.freelance.event.exception;

/**
 * Thrown by an {@link com.freelance.event.consumer.EventHandler} to signal that event processing
 * should be paused, delayed, and retried at a later time in accordance with backoff policies.
 * <p>
 * Allows consumers across any domain module (e.g. alerts awaiting human replies, payment gateway rate limits)
 * to explicitly request a delayed retry from the Event Management Engine without treating it as an unhandled failure.
 * </p>
 */
public class EventDelayedException extends RuntimeException {

    private final Long customDelayMillis;

    public EventDelayedException(String message) {
        super(message);
        this.customDelayMillis = null;
    }

    public EventDelayedException(String message, long customDelayMillis) {
        super(message);
        this.customDelayMillis = customDelayMillis;
    }

    public EventDelayedException(String message, Throwable cause) {
        super(message, cause);
        this.customDelayMillis = null;
    }

    public EventDelayedException(String message, long customDelayMillis, Throwable cause) {
        super(message, cause);
        this.customDelayMillis = customDelayMillis;
    }

    public Long getCustomDelayMillis() {
        return customDelayMillis;
    }
}
