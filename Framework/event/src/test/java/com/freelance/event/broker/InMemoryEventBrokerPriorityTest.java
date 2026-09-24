package com.freelance.event.broker;

import com.freelance.event.TestConsumer;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEventBrokerPriorityTest {

    private InMemoryEventBroker broker;

    @BeforeEach
    void setUp() {
        broker = new InMemoryEventBroker();
    }

    @AfterEach
    void tearDown() {
        if (broker != null) {
            broker.shutdown(1000);
        }
    }

    @Test
    @DisplayName("High priority events preempt queued Normal and Low priority events")
    void testPriorityPreemption() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_PRIORITY_TOPIC;
        int lowCount = 20;
        CountDownLatch latch = new CountDownLatch(lowCount + 1);
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Publish 20 Low priority tasks
        for (int i = 1; i <= lowCount; i++) {
            broker.publish(EventMessage.of(destination, "LOW-" + i, EventPriority.LOW));
        }

        // Publish 1 High priority task
        broker.publish(EventMessage.of(destination, "HIGH-URGENT", EventPriority.HIGH));

        // Subscribe consumer with concurrency = 1 to verify serial priority processing
        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .rateLimitPerSecond(0.0)
                .build();

        broker.subscribe(destination, config, String.class, msg -> {
            executionOrder.add(msg.getPayload());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All tasks should be processed within 5 seconds");

        // The HIGH priority event must be the very first task executed
        assertEquals("HIGH-URGENT", executionOrder.get(0), "HIGH priority event must preempt LOW priority events");
    }

    @Test
    @DisplayName("Events with the same priority maintain strict FIFO ordering")
    void testFifoOrdering() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_FIFO_TOPIC;
        int total = 10;
        CountDownLatch latch = new CountDownLatch(total);
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= total; i++) {
            broker.publish(EventMessage.of(destination, "MSG-" + i, EventPriority.NORMAL));
        }

        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .build();

        broker.subscribe(destination, config, String.class, msg -> {
            executionOrder.add(msg.getPayload());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        for (int i = 1; i <= total; i++) {
            assertEquals("MSG-" + i, executionOrder.get(i - 1), "Events must be processed in FIFO sequence");
        }
    }

    @Test
    @DisplayName("Multi-tenant queues ensure fair-share round-robin interleaving so no tenant is starved")
    void testMultiTenantFairShareInterleaving() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_FIFO_TOPIC;
        int countPerTenant = 10;
        CountDownLatch latch = new CountDownLatch(countPerTenant * 2);
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        // Pre-enqueue 10 tasks for Tenant A
        for (int i = 1; i <= countPerTenant; i++) {
            broker.publish(EventMessage.of(destination, "tenant_a", "A-" + i, EventPriority.NORMAL));
        }

        // Pre-enqueue 10 tasks for Tenant B
        for (int i = 1; i <= countPerTenant; i++) {
            broker.publish(EventMessage.of(destination, "tenant_b", "B-" + i, EventPriority.NORMAL));
        }

        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .build();

        broker.subscribe(destination, config, String.class, msg -> {
            executionOrder.add(msg.getPayload());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(20, executionOrder.size());

        // Verify that Tenant B tasks are interleaved and start executing within the first few rounds
        boolean foundBInFirstThree = executionOrder.subList(0, 3).stream().anyMatch(s -> s.startsWith("B-"));
        boolean foundAInFirstThree = executionOrder.subList(0, 3).stream().anyMatch(s -> s.startsWith("A-"));
        assertTrue(foundBInFirstThree, "Tenant B tasks must not be starved by Tenant A");
        assertTrue(foundAInFirstThree, "Tenant A tasks must not be starved by Tenant B");
    }

    @Test
    @DisplayName("publishDelayed with delay <= 0 publishes immediately")
    void testPublishDelayedZeroOrNegative() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_FIFO_TOPIC;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();

        broker.subscribe(destination, ConsumerConfig.builder().concurrency(1).build(), String.class, msg -> {
            result.set(msg.getPayload());
            latch.countDown();
        });

        broker.publishDelayed(EventMessage.of(destination, "DELAYED_ZERO", EventPriority.NORMAL), 0);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("DELAYED_ZERO", result.get());
    }

    @Test
    @DisplayName("Duplicate subscriber registration throws IllegalStateException")
    void testDuplicateSubscriberThrows() {
        TestConsumer destination = TestConsumer.TEST_FIFO_TOPIC;
        ConsumerConfig config = ConsumerConfig.builder().concurrency(1).build();

        broker.subscribe(destination, config, String.class, msg -> {});
        assertThrows(IllegalStateException.class, () ->
                broker.subscribe(destination, config, String.class, msg -> {})
        );
    }

    @Test
    @DisplayName("Publishing after shutdown throws IllegalStateException")
    void testPublishAfterShutdownThrows() {
        broker.shutdown(500);
        assertThrows(IllegalStateException.class, () ->
                broker.publish(EventMessage.of(TestConsumer.TEST_FIFO_TOPIC, "AFTER_SHUTDOWN", EventPriority.NORMAL))
        );
        // publishDelayed when shutdown returns gracefully without throwing
        assertDoesNotThrow(() ->
                broker.publishDelayed(EventMessage.of(TestConsumer.TEST_FIFO_TOPIC, "DELAY_AFTER_SHUTDOWN", EventPriority.NORMAL), 100)
        );
    }

    @Test
    @DisplayName("Broker start and shutdown are idempotent")
    void testStartAndShutdownIdempotent() {
        assertDoesNotThrow(() -> broker.start());
        assertDoesNotThrow(() -> broker.shutdown(100));
        assertDoesNotThrow(() -> broker.shutdown(100));
    }
}
