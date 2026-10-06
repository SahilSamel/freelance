package com.freelance.event.engine;

import com.freelance.event.TestConsumer;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventManagementEngine Emission Unit Tests")
class EventManagementEngineTest {

    private InMemoryEventBroker testBroker;

    @BeforeEach
    void setUp() {
        testBroker = new InMemoryEventBroker();
        EventRegistry.getInstance().setBrokerForTesting(testBroker);
    }

    @AfterEach
    void tearDown() {
        EventRegistry.getInstance().shutdown(500);
        MDC.clear();
    }

    @Test
    @DisplayName("Private constructor reflection test")
    void testPrivateConstructor() throws Exception {
        Constructor<EventManagementEngine> ctor = EventManagementEngine.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        EventManagementEngine engine = ctor.newInstance();
        assertNotNull(engine);
    }

    @Test
    @DisplayName("emit with destination, payload, and normal priority")
    void testEmitDestinationAndPayload() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedPayload = new AtomicReference<>();

        EventRegistry.getInstance().registerConsumer(
                TestConsumer.TEST_FIFO_TOPIC,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    receivedPayload.set(msg.getPayload());
                    latch.countDown();
                }
        );

        EventManagementEngine.emit(TestConsumer.TEST_FIFO_TOPIC, "Hello Event Engine", EventPriority.NORMAL);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("Hello Event Engine", receivedPayload.get());
    }

    @Test
    @DisplayName("emit with custom MDC traceId and priority")
    void testEmitWithMdcTraceId() throws InterruptedException {
        MDC.put("traceId", "custom-trace-999");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedTrace = new AtomicReference<>();

        EventRegistry.getInstance().registerConsumer(
                TestConsumer.TEST_PRIORITY_TOPIC,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    receivedTrace.set(msg.getTraceId());
                    latch.countDown();
                }
        );

        EventManagementEngine.emit(TestConsumer.TEST_PRIORITY_TOPIC, "High Priority Task", EventPriority.HIGH);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("custom-trace-999", receivedTrace.get());
    }

    @Test
    @DisplayName("emit with explicit tenantId")
    void testEmitWithExplicitTenantId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedTenant = new AtomicReference<>();

        EventRegistry.getInstance().registerConsumer(
                TestConsumer.TEST_FIFO_TOPIC,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    receivedTenant.set(msg.getTenantId());
                    latch.countDown();
                }
        );

        EventManagementEngine.emit(TestConsumer.TEST_FIFO_TOPIC, "tenant_custom_123", "payload", EventPriority.NORMAL);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("tenant_custom_123", receivedTenant.get());
    }

    @Test
    @DisplayName("emitBatch publishes list of payloads successfully")
    void testEmitBatch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        java.util.List<String> received = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        EventRegistry.getInstance().registerConsumer(
                TestConsumer.TEST_FIFO_TOPIC,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    received.add(msg.getPayload());
                    latch.countDown();
                }
        );

        EventManagementEngine.emitBatch(
                TestConsumer.TEST_FIFO_TOPIC,
                java.util.List.of("Batch-1", "Batch-2", "Batch-3"),
                EventPriority.NORMAL
        );

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(3, received.size());
        assertEquals("Batch-1", received.get(0));
        assertEquals("Batch-2", received.get(1));
        assertEquals("Batch-3", received.get(2));
    }

    @Test
    @DisplayName("emitBatch with explicit tenantId populates tenantId in all messages")
    void testEmitBatchWithTenantId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        java.util.List<String> tenants = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        EventRegistry.getInstance().registerConsumer(
                TestConsumer.TEST_FIFO_TOPIC,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    tenants.add(msg.getTenantId());
                    latch.countDown();
                }
        );

        EventManagementEngine.emitBatch(
                TestConsumer.TEST_FIFO_TOPIC,
                "tenant_batch_gym",
                java.util.List.of("M1", "M2"),
                EventPriority.HIGH
        );

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(2, tenants.size());
        assertEquals("tenant_batch_gym", tenants.get(0));
        assertEquals("tenant_batch_gym", tenants.get(1));
    }

    @Test
    @DisplayName("emitBatch with null or empty list is a no-op")
    void testEmitBatchNullOrEmpty() {
        assertDoesNotThrow(() -> EventManagementEngine.emitBatch(TestConsumer.TEST_FIFO_TOPIC, (java.util.List<String>) null, EventPriority.NORMAL));
        assertDoesNotThrow(() -> EventManagementEngine.emitBatch(TestConsumer.TEST_FIFO_TOPIC, java.util.Collections.emptyList(), EventPriority.NORMAL));
    }
}
