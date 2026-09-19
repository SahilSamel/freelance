package com.freelance.event.engine;

import com.freelance.event.IConsumer;
import com.freelance.event.TestConsumer;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.model.EventMessage;
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

@DisplayName("EventManagementEngine Lifecycle & Emission Unit Tests")
class EventManagementEngineTest {

    private InMemoryEventBroker testBroker;

    @BeforeEach
    void setUp() {
        testBroker = new InMemoryEventBroker();
        EventManagementEngine.getInstance().setBrokerForTesting(testBroker);
    }

    @AfterEach
    void tearDown() {
        EventManagementEngine.getInstance().shutdown(500);
        MDC.clear();
    }

    @Test
    @DisplayName("Singleton instance and private constructor reflection test")
    void testSingletonAndConstructor() throws Exception {
        assertNotNull(EventManagementEngine.getInstance());

        Constructor<EventManagementEngine> ctor = EventManagementEngine.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        EventManagementEngine engine = ctor.newInstance();
        assertNotNull(engine);
    }

    @Test
    @DisplayName("init() idempotency and double invocation")
    void testInitIdempotence() {
        assertDoesNotThrow(() -> EventManagementEngine.getInstance().init());
        assertDoesNotThrow(() -> EventManagementEngine.getInstance().init());
    }

    @Test
    @DisplayName("emit with destination and payload with normal priority")
    void testEmitDestinationAndPayload() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedPayload = new AtomicReference<>();

        EventManagementEngine.getInstance().registerConsumer(
                TestConsumer.TEST_FIFO_TOPIC,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    receivedPayload.set(msg.getPayload());
                    latch.countDown();
                }
        );

        EventManagementEngine.emit(TestConsumer.TEST_FIFO_TOPIC, "Hello Event Engine");
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("Hello Event Engine", receivedPayload.get());
    }

    @Test
    @DisplayName("emit with custom MDC traceId and priority")
    void testEmitWithMdcTraceId() throws InterruptedException {
        MDC.put("traceId", "custom-trace-999");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedTrace = new AtomicReference<>();

        EventManagementEngine.getInstance().registerConsumer(
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
    @DisplayName("emit with custom EventMessage envelope and null envelope guard")
    void testEmitEnvelope() throws InterruptedException {
        assertDoesNotThrow(() -> EventManagementEngine.emit((EventMessage<String>) null));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedPayload = new AtomicReference<>();

        EventManagementEngine.getInstance().registerConsumer(
                TestConsumer.TEST_FIFO_TOPIC,
                ConsumerConfig.builder().concurrency(1).build(),
                String.class,
                msg -> {
                    receivedPayload.set(msg.getPayload());
                    latch.countDown();
                }
        );

        EventMessage<String> msg = EventMessage.of(TestConsumer.TEST_FIFO_TOPIC, "Envelope Message", EventPriority.NORMAL);
        EventManagementEngine.emit(msg);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("Envelope Message", receivedPayload.get());
    }

    @Test
    @DisplayName("shutdown handles null and active brokers gracefully")
    void testShutdownGraceful() {
        assertDoesNotThrow(() -> EventManagementEngine.getInstance().shutdown(100));
        assertDoesNotThrow(() -> EventManagementEngine.getInstance().shutdown(100));
    }
}
