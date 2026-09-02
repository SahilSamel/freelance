package com.freelance.event.engine;

import com.freelance.event.TestConsumer;
import com.freelance.event.broker.impl.InMemoryEventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RetryAndDeadLetterTest {

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
    @DisplayName("Transient exceptions trigger exponential backoff retries and succeed on subsequent attempts")
    void testTransientFailureRetrySuccess() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_TRANSIENT_RETRY;
        AtomicInteger attempts = new AtomicInteger(0);
        CountDownLatch successLatch = new CountDownLatch(1);

        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .maxRetries(3)
                .backoffMillis(50)
                .backoffMultiplier(2.0)
                .build();

        EventManagementEngine.getInstance().registerConsumer(
                destination,
                config,
                String.class,
                msg -> {
                    int attempt = attempts.incrementAndGet();
                    if (attempt == 1) {
                        throw new IOException("Simulated temporary network glitch");
                    }
                    // Attempt 2 succeeds
                    successLatch.countDown();
                }
        );

        EventManagementEngine.emit(destination, "retry-payload", EventPriority.NORMAL);

        assertTrue(successLatch.await(5, TimeUnit.SECONDS), "Task should succeed on retry attempt within 5 seconds");
        assertEquals(2, attempts.get(), "Handler should have been invoked exactly twice (1 failure + 1 success)");
    }

    @Test
    @DisplayName("Permanent exceptions exhaust retries and route the message to the overridden onDeadLetter handler")
    void testPermanentFailureDeadLetterRouting() throws InterruptedException {
        TestConsumer destination = TestConsumer.TEST_PERMANENT_DLQ;
        AtomicInteger attempts = new AtomicInteger(0);
        CountDownLatch dlqLatch = new CountDownLatch(1);
        AtomicReference<EventMessage<String>> deadLetterMessage = new AtomicReference<>();
        AtomicReference<Throwable> deadLetterCause = new AtomicReference<>();

        int maxRetries = 2;
        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .maxRetries(maxRetries)
                .backoffMillis(20)
                .backoffMultiplier(1.5)
                .build();

        EventManagementEngine.getInstance().registerConsumer(
                destination,
                config,
                String.class,
                new EventHandler<String>() {
                    @Override
                    public void handle(EventMessage<String> message) {
                        attempts.incrementAndGet();
                        throw new RuntimeException("Permanent fatal business error");
                    }

                    @Override
                    public void onDeadLetter(EventMessage<String> message, Throwable cause) {
                        deadLetterMessage.set(message);
                        deadLetterCause.set(cause);
                        dlqLatch.countDown();
                    }
                }
        );

        EventManagementEngine.emit(destination, "poison-pill-payload", EventPriority.HIGH);

        assertTrue(dlqLatch.await(5, TimeUnit.SECONDS), "Dead letter handler should be invoked after retries exhaust");
        assertEquals(maxRetries + 1, attempts.get(), "Should execute initial try + 2 retries = 3 attempts total");

        assertNotNull(deadLetterMessage.get());
        assertEquals("poison-pill-payload", deadLetterMessage.get().getPayload());
        assertEquals(maxRetries, deadLetterMessage.get().getRetryCount());
        assertNotNull(deadLetterCause.get());
        assertTrue(deadLetterCause.get().getMessage().contains("Permanent fatal business error"));
    }
}
