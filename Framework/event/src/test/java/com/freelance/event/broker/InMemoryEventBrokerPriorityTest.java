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
}
