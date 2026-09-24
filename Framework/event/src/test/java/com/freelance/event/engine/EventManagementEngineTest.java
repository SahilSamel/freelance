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
}
