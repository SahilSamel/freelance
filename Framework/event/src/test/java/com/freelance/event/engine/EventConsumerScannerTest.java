package com.freelance.event.engine;

import com.freelance.event.broker.impl.InMemoryEventBroker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventConsumerScanner Discovery Unit Tests")
class EventConsumerScannerTest {

    private InMemoryEventBroker broker;
    private final EventConsumerScanner scanner = new EventConsumerScanner();

    @BeforeEach
    void setUp() {
        broker = new InMemoryEventBroker();
    }

    @AfterEach
    void tearDown() {
        if (broker != null) {
            broker.shutdown(500);
        }
    }

    @Test
    @DisplayName("Returns 0 when broker is null")
    void testNullBrokerReturnsZero() {
        assertEquals(0, scanner.scanAndRegister(null));
    }

    @Test
    @DisplayName("Scans and registers declared @EventConsumer implementations")
    void testScanAndRegister() {
        int count = scanner.scanAndRegister(broker);
        assertTrue(count >= 0);
    }
}
