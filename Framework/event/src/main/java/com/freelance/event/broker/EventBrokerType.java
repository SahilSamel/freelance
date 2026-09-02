package com.freelance.event.broker;

/**
 * Enumerates all recognized message broker drivers supported by the event management framework.
 * <p>
 * Any new implementation of {@link EventBroker} <b>MUST</b> declare a corresponding entry in this enum
 * and register its factory instantiation in {@link EventBrokerFactory}.
 * </p>
 */
public enum EventBrokerType {

    /**
     * JVM-local message broker backed by {@link java.util.concurrent.PriorityBlockingQueue}.
     */
    IN_MEMORY,

    /**
     * Distributed Redis message broker backed by Redis Sorted Sets (ZSET).
     */
    REDIS;

    /**
     * Resolves a string value to an {@link EventBrokerType}, defaulting to {@link #IN_MEMORY}
     * if null, empty, or unrecognized.
     *
     * @param value the string representation of the broker type
     * @return the resolved {@link EventBrokerType}
     */
    public static EventBrokerType fromString(String value) {
        if (value == null || value.isBlank()) {
            return IN_MEMORY;
        }
        try {
            return EventBrokerType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return IN_MEMORY;
        }
    }
}
