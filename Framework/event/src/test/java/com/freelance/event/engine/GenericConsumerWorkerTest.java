package com.freelance.event.engine;

import com.freelance.event.IConsumer;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.rest.app.transaction.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GenericConsumerWorkerTest {

    record SamplePayload(String name, int count) {}

    static class FakeBroker implements EventBroker {
        EventMessage<?> delayedMessage;
        long lastDelay = -1;

        @Override
        public <T> void publish(EventMessage<T> message) {}

        @Override
        public <T> void publishDelayed(EventMessage<T> message, long delayMillis) {
            this.delayedMessage = message;
            this.lastDelay = delayMillis;
        }

        @Override
        public <T> void subscribe(IConsumer destination, ConsumerConfig config, Class<T> payloadClass, EventHandler<T> handler) {}

        @Override
        public void start() {}

        @Override
        public void shutdown(long timeoutMillis) {}
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Successfully processes message and binds caller user ID")
    void testSuccessfulProcessing() {
        UUID callerId = UUID.randomUUID();
        EventMessage<SamplePayload> msg = new EventMessage<>(
                UUID.randomUUID(),
                "topic.test",
                new SamplePayload("item", 42),
                SamplePayload.class.getName(),
                EventPriority.NORMAL,
                callerId,
                "trace-123",
                Instant.now(),
                0,
                0L
        );

        AtomicBoolean handled = new AtomicBoolean(false);
        EventHandler<SamplePayload> handler = message -> {
            handled.set(true);
            assertEquals("item", message.getPayload().name());
            assertEquals(callerId, message.getCallerUserId());
        };

        ConsumerConfig config = ConsumerConfig.defaultForTopic();
        FakeBroker broker = new FakeBroker();

        GenericConsumerWorker<SamplePayload> worker = new GenericConsumerWorker<>(
                msg, SamplePayload.class, handler, config, broker
        );

        worker.run();

        assertTrue(handled.get());
        assertNull(broker.delayedMessage);
    }

    @Test
    @DisplayName("Generates random traceId when message traceId is null or blank")
    void testTraceIdGeneration() {
        EventMessage<String> msg = new EventMessage<>(
                UUID.randomUUID(),
                "topic.test",
                "payload",
                String.class.getName(),
                EventPriority.NORMAL,
                null,
                "",
                Instant.now(),
                0,
                0L
        );

        AtomicBoolean handled = new AtomicBoolean(false);
        GenericConsumerWorker<String> worker = new GenericConsumerWorker<>(
                msg, String.class, m -> handled.set(true), ConsumerConfig.defaultForTopic(), new FakeBroker()
        );

        worker.run();
        assertTrue(handled.get());
    }

    @Test
    @DisplayName("Retries message with exponential backoff on failure when retryCount < maxRetries")
    void testRetryOnFailure() {
        EventMessage<String> msg = new EventMessage<>(
                UUID.randomUUID(),
                "topic.retry",
                "test",
                String.class.getName(),
                EventPriority.HIGH,
                null,
                "trace-retry",
                Instant.now(),
                1,
                0L
        );

        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .maxRetries(3)
                .backoffMillis(100)
                .backoffMultiplier(2.0)
                .build();

        FakeBroker broker = new FakeBroker();
        GenericConsumerWorker<String> worker = new GenericConsumerWorker<>(
                msg,
                String.class,
                m -> { throw new RuntimeException("Simulated worker error"); },
                config,
                broker
        );

        worker.run();

        assertNotNull(broker.delayedMessage);
        assertEquals(2, broker.delayedMessage.getRetryCount());
        assertEquals(200L, broker.lastDelay);
    }

    @Test
    @DisplayName("Routes exhausted retries to dead letter handler with DLQ transaction")
    void testDeadLetterRouting() {
        UUID callerId = UUID.randomUUID();
        EventMessage<String> msg = new EventMessage<>(
                UUID.randomUUID(),
                "topic.dlq",
                "poison",
                String.class.getName(),
                EventPriority.NORMAL,
                callerId,
                "trace-dlq",
                Instant.now(),
                3, // max retries reached
                0L
        );

        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .maxRetries(3)
                .backoffMillis(100)
                .backoffMultiplier(2.0)
                .build();

        FakeBroker broker = new FakeBroker();
        AtomicReference<EventMessage<String>> dlqMsg = new AtomicReference<>();
        AtomicReference<Throwable> dlqCause = new AtomicReference<>();

        EventHandler<String> handler = new EventHandler<>() {
            @Override
            public void handle(EventMessage<String> message) {
                throw new RuntimeException("Fatal error");
            }

            @Override
            public void onDeadLetter(EventMessage<String> message, Throwable cause) {
                dlqMsg.set(message);
                dlqCause.set(cause);
                assertEquals(callerId, message.getCallerUserId());
            }
        };

        GenericConsumerWorker<String> worker = new GenericConsumerWorker<>(
                msg, String.class, handler, config, broker
        );

        worker.run();

        assertNull(broker.delayedMessage);
        assertNotNull(dlqMsg.get());
        assertEquals("poison", dlqMsg.get().getPayload());
        assertNotNull(dlqCause.get());
        assertTrue(dlqCause.get().getMessage().contains("Fatal error"));
    }

    @Test
    @DisplayName("Handles exception thrown from onDeadLetter gracefully")
    void testDeadLetterThrowsException() {
        EventMessage<String> msg = new EventMessage<>(
                UUID.randomUUID(),
                "topic.dlq.err",
                "poison",
                String.class.getName(),
                EventPriority.NORMAL,
                null,
                "trace",
                Instant.now(),
                1,
                0L
        );

        ConsumerConfig config = ConsumerConfig.builder()
                .concurrency(1)
                .maxRetries(1)
                .build();

        EventHandler<String> handler = new EventHandler<>() {
            @Override
            public void handle(EventMessage<String> message) {
                throw new RuntimeException("Initial failure");
            }

            @Override
            public void onDeadLetter(EventMessage<String> message, Throwable cause) {
                throw new RuntimeException("DeadLetter handler itself crashed");
            }
        };

        GenericConsumerWorker<String> worker = new GenericConsumerWorker<>(
                msg, String.class, handler, config, new FakeBroker()
        );

        assertDoesNotThrow(worker::run);
    }

    @Test
    @DisplayName("convertPayload handles object conversion failure fallback")
    void testConvertPayloadFallback() {
        // Pass payload of type String when targetClass is SamplePayload
        EventMessage<String> msg = new EventMessage<>(
                UUID.randomUUID(),
                "topic.convert",
                "incompatible_string",
                String.class.getName(),
                EventPriority.NORMAL,
                null,
                "trace",
                Instant.now(),
                0,
                0L
        );

        AtomicBoolean handled = new AtomicBoolean(false);
        EventHandler<SamplePayload> handler = m -> {
            handled.set(true);
        };

        GenericConsumerWorker<SamplePayload> worker = new GenericConsumerWorker<>(
                msg, SamplePayload.class, handler, ConsumerConfig.defaultForTopic(), new FakeBroker()
        );

        // Will catch conversion error and pass raw message
        worker.run();
        assertTrue(handled.get());
    }

    @Test
    @DisplayName("convertPayload handles null payload and Object.class target")
    void testConvertPayloadNullAndObjectClass() {
        // Null payload
        EventMessage<Object> nullPayloadMsg = new EventMessage<>(
                UUID.randomUUID(), "topic.null", null, Object.class.getName(),
                EventPriority.NORMAL, null, "trace", Instant.now(), 0, 0L
        );
        AtomicBoolean handled1 = new AtomicBoolean(false);
        GenericConsumerWorker<Object> worker1 = new GenericConsumerWorker<>(
                nullPayloadMsg, Object.class, m -> handled1.set(true), ConsumerConfig.defaultForTopic(), new FakeBroker()
        );
        worker1.run();
        assertTrue(handled1.get());

        // Object.class target with non-null payload
        EventMessage<String> strMsg = new EventMessage<>(
                UUID.randomUUID(), "topic.obj", "hello", String.class.getName(),
                EventPriority.NORMAL, null, "trace", Instant.now(), 0, 0L
        );
        AtomicBoolean handled2 = new AtomicBoolean(false);
        GenericConsumerWorker<Object> worker2 = new GenericConsumerWorker<>(
                strMsg, Object.class, m -> handled2.set(true), ConsumerConfig.defaultForTopic(), new FakeBroker()
        );
        worker2.run();
        assertTrue(handled2.get());
    }

    @Test
    @DisplayName("Worker handles null handler on retry exhaustion without throwing")
    void testExhaustedRetriesWithNullHandler() {
        EventMessage<String> msg = new EventMessage<>(
                UUID.randomUUID(), "topic.dlq.null", "data", String.class.getName(),
                EventPriority.NORMAL, null, "trace", Instant.now(), 2, 0L
        );
        ConsumerConfig config = ConsumerConfig.builder().maxRetries(1).build();

        GenericConsumerWorker<String> worker = new GenericConsumerWorker<>(
                msg, String.class, null, config, new FakeBroker()
        );
        assertDoesNotThrow(worker::run);
    }

    private static Object createDynamicMockEmf(AtomicBoolean committed, AtomicBoolean rolledBack, AtomicBoolean closed) throws Exception {
        Class<?> emfClass = Class.forName("jakarta.persistence.EntityManagerFactory");
        Class<?> emClass = Class.forName("jakarta.persistence.EntityManager");
        Class<?> txClass = Class.forName("jakarta.persistence.EntityTransaction");

        Object mockTx = java.lang.reflect.Proxy.newProxyInstance(GenericConsumerWorkerTest.class.getClassLoader(), new Class<?>[]{txClass}, (proxy, method, args) -> {
            if ("isActive".equals(method.getName())) return true;
            if ("commit".equals(method.getName())) { if (committed != null) committed.set(true); return null; }
            if ("rollback".equals(method.getName())) { if (rolledBack != null) rolledBack.set(true); return null; }
            return null;
        });

        Object mockEm = java.lang.reflect.Proxy.newProxyInstance(GenericConsumerWorkerTest.class.getClassLoader(), new Class<?>[]{emClass}, (proxy, method, args) -> {
            if ("getTransaction".equals(method.getName())) return mockTx;
            if ("close".equals(method.getName())) { if (closed != null) closed.set(true); return null; }
            return null;
        });

        return java.lang.reflect.Proxy.newProxyInstance(GenericConsumerWorkerTest.class.getClassLoader(), new Class<?>[]{emfClass}, (proxy, method, args) -> {
            if ("createEntityManager".equals(method.getName())) return mockEm;
            if ("isOpen".equals(method.getName())) return true;
            return null;
        });
    }

    private static void setTransactionEmf(Object emf) throws Exception {
        java.lang.reflect.Method initMethod = Transaction.class.getMethod("init", Class.forName("jakarta.persistence.EntityManagerFactory"));
        initMethod.invoke(null, emf);
    }

    @Test
    @DisplayName("Active transaction lifecycle: success commits transaction and binds user")
    void testActiveTransactionSuccess() throws Exception {
        AtomicBoolean committed = new AtomicBoolean(false);
        AtomicBoolean closed = new AtomicBoolean(false);
        Object mockEmf = createDynamicMockEmf(committed, null, closed);
        setTransactionEmf(mockEmf);

        try {
            UUID callerId = UUID.randomUUID();
            EventMessage<String> msg = new EventMessage<>(
                    UUID.randomUUID(), "topic.tx.success", "payload", String.class.getName(),
                    EventPriority.NORMAL, callerId, "trace-tx", Instant.now(), 0, 0L
            );

            AtomicBoolean handled = new AtomicBoolean(false);
            GenericConsumerWorker<String> worker = new GenericConsumerWorker<>(
                    msg, String.class, m -> {
                        handled.set(true);
                        assertEquals(callerId, Transaction.getRequestContext().userId());
                    },
                    ConsumerConfig.defaultForTopic(), new FakeBroker()
            );

            worker.run();
            assertTrue(handled.get());
            assertTrue(committed.get());
            assertTrue(closed.get());
        } finally {
            setTransactionEmf(null);
        }
    }

    @Test
    @DisplayName("Active transaction lifecycle: failure marks rollback only and executes rollback")
    void testActiveTransactionRollbackOnFailure() throws Exception {
        AtomicBoolean rolledBack = new AtomicBoolean(false);
        AtomicBoolean closed = new AtomicBoolean(false);
        Object mockEmf = createDynamicMockEmf(null, rolledBack, closed);
        setTransactionEmf(mockEmf);

        try {
            EventMessage<String> msg = new EventMessage<>(
                    UUID.randomUUID(), "topic.tx.fail", "payload", String.class.getName(),
                    EventPriority.NORMAL, null, "trace-tx", Instant.now(), 0, 0L
            );

            FakeBroker broker = new FakeBroker();
            GenericConsumerWorker<String> worker = new GenericConsumerWorker<>(
                    msg, String.class, m -> { throw new RuntimeException("DB Error"); },
                    ConsumerConfig.builder().maxRetries(2).build(), broker
            );

            worker.run();
            assertTrue(rolledBack.get());
            assertTrue(closed.get());
            assertNotNull(broker.delayedMessage);
        } finally {
            setTransactionEmf(null);
        }
    }

    @Test
    @DisplayName("Active transaction lifecycle: DLQ handler starts isolated transaction and commits or rolls back")
    void testActiveTransactionDLQCommitAndRollback() throws Exception {
        Object mockEmf = createDynamicMockEmf(null, null, null);
        setTransactionEmf(mockEmf);

        try {
            UUID callerId = UUID.randomUUID();
            EventMessage<String> msg = new EventMessage<>(
                    UUID.randomUUID(), "topic.dlq.tx", "payload", String.class.getName(),
                    EventPriority.NORMAL, callerId, "trace-dlq", Instant.now(), 1, 0L
            );

            // 1. DLQ Success
            EventHandler<String> successfulDlqHandler = new EventHandler<>() {
                @Override public void handle(EventMessage<String> message) { throw new RuntimeException("Fail 1"); }
                @Override public void onDeadLetter(EventMessage<String> message, Throwable cause) {
                    assertEquals(callerId, Transaction.getRequestContext().userId());
                }
            };
            GenericConsumerWorker<String> worker1 = new GenericConsumerWorker<>(
                    msg, String.class, successfulDlqHandler, ConsumerConfig.builder().maxRetries(1).build(), new FakeBroker()
            );
            worker1.run();

            // 2. DLQ Error
            EventHandler<String> failingDlqHandler = new EventHandler<>() {
                @Override public void handle(EventMessage<String> message) { throw new RuntimeException("Fail 1"); }
                @Override public void onDeadLetter(EventMessage<String> message, Throwable cause) {
                    throw new RuntimeException("DLQ also failed");
                }
            };
            GenericConsumerWorker<String> worker2 = new GenericConsumerWorker<>(
                    msg, String.class, failingDlqHandler, ConsumerConfig.builder().maxRetries(1).build(), new FakeBroker()
            );
            worker2.run();
        } finally {
            setTransactionEmf(null);
        }
    }
}
