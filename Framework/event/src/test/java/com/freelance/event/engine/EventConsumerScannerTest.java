package com.freelance.event.engine;

import com.freelance.consumer.test.ValidTestConsumer;
import com.freelance.event.TestConsumer;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EventConsumerScannerTest {

    private InMemoryEventBroker broker;

    @BeforeEach
    void setUp() {
        broker = new InMemoryEventBroker();
        EventManagementEngine.getInstance().setBrokerForTesting(broker);
    }

    @AfterEach
    void tearDown() {
        EventManagementEngine.getInstance().shutdown(1000);
    }

    @Test
    @DisplayName("EventConsumerScanner discovers annotated consumer in com.freelance.consumer and registers it")
    void testAnnotationDiscoveryAndDispatch() throws InterruptedException {
        EventConsumerScanner scanner = new EventConsumerScanner();
        int registered = scanner.scanAndRegister(broker);

        assertTrue(registered >= 1, "Should discover ValidTestConsumer in com.freelance.consumer");

        EventManagementEngine.emit(TestConsumer.SCAN_TEST_DESTINATION, new ValidTestConsumer.ScanTestDTO("Auto-discovery works!"), EventPriority.HIGH);

        assertTrue(ValidTestConsumer.testLatch.await(5, TimeUnit.SECONDS), "Auto-discovered consumer should receive and process message");
        assertNotNull(ValidTestConsumer.receivedDto.get());
        assertEquals("Auto-discovery works!", ValidTestConsumer.receivedDto.get().text());
    }
}
