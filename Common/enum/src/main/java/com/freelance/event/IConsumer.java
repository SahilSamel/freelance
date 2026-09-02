package com.freelance.event;

/**
 * Defines the contract for asynchronous event consumer destinations (topics) across the system.
 * <p>
 * Enums representing system, module, or test event topics implement this interface
 * to provide type-safe topic references when emitting events and managing subscriptions.
 * </p>
 */
@FunctionalInterface
public interface IConsumer {

    /**
     * Retrieves the unique destination topic name.
     *
     * @return unique destination topic string
     */
    String getDestination();

    /**
     * Creates an {@link IConsumer} instance wrapping a destination name string.
     *
     * @param destination the destination name
     * @return IConsumer instance
     */
    static IConsumer of(String destination) {
        if (destination == null) return null;
        return () -> destination;
    }
}
