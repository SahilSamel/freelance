package com.freelance.event.engine;

import com.freelance.event.TestConsumer;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventRegistry Unit Tests")
class EventRegistryTest {

    private EventRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EventRegistry();
    }

    @AfterEach
    void tearDown() {
        registry.shutdown(500);
    }

    @Test
    @DisplayName("init idempotence and isInitialized state")
    void testInitIdempotence() {
        assertFalse(registry.isInitialized());
        registry.init();
        assertTrue(registry.isInitialized());
        // Second call is safe no-op
        assertDoesNotThrow(() -> registry.init());
        assertTrue(registry.isInitialized());
    }

    @Test
    @DisplayName("shutdown closes active broker and resets initialized state")
    void testShutdown() {
        registry.init();
        assertTrue(registry.isInitialized());
        registry.shutdown(100);
        assertFalse(registry.isInitialized());
    }

    @Test
    @DisplayName("registerConsumer throws on null destination and registers successfully")
    void testRegisterConsumer() {
        assertThrows(NullPointerException.class, () ->
                registry.registerConsumer(null, ConsumerConfig.builder().build(), String.class, msg -> {}));

        assertDoesNotThrow(() ->
                registry.registerConsumer(TestConsumer.TEST_FIFO_TOPIC, ConsumerConfig.builder().build(), String.class, msg -> {}));
    }

    @Test
    @DisplayName("setBrokerForTesting allows swapping test broker")
    void testSetBrokerForTesting() {
        InMemoryEventBroker testBroker = new InMemoryEventBroker();
        registry.setBrokerForTesting(testBroker);
        assertTrue(registry.isInitialized());
        assertSame(testBroker, registry.getOrCreateBroker());
    }
}
