package com.freelance.event;

/**
 * Enumerates all recognized system-level asynchronous event consumer destinations.
 * <p>
 * Implements {@link IConsumer} to provide compile-time safety and centralized discovery
 * for production background tasks.
 * </p>
 */
public enum SystemConsumer implements IConsumer {
    ;

    @Override
    public String getDestination() {
        return name();
    }
}
