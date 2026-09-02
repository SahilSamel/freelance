package com.freelance.event;

/**
 * Enumerates test-specific event consumer destinations for unit and integration testing.
 */
public enum TestConsumer implements IConsumer {

    SCAN_TEST_DESTINATION,
    TEST_TRANSIENT_RETRY,
    TEST_PERMANENT_DLQ,
    TEST_PRIORITY_TOPIC,
    TEST_FIFO_TOPIC,
    TEST_IDENTITY_TOPIC,
    ILLEGAL_PACKAGE_DESTINATION;

    @Override
    public String getDestination() {
        return name();
    }
}
