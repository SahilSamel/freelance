package com.freelance.event.broker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freelance.event.IConsumer;
import com.freelance.event.broker.impl.RedisEventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.resps.Tuple;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RedisEventBroker Tests")
class RedisEventBrokerTest {

    private enum TestTopic implements IConsumer {
        SAMPLE_TOPIC("test.redis.topic"),
        SECOND_TOPIC("test.redis.second");

        private final String destination;
        TestTopic(String destination) { this.destination = destination; }
        @Override public String getDestination() { return destination; }
    }

    static class FakeJedis extends Jedis {
        final List<String> zaddKeys = new ArrayList<>();
        final List<Double> zaddScores = new ArrayList<>();
        final List<String> zaddMembers = new ArrayList<>();
        final List<Tuple> readyTuples = new ArrayList<>();
        final List<String> zremMembers = new ArrayList<>();
        final List<Tuple> popTuples = new ArrayList<>();
        final Set<String> saddMembers = new HashSet<>();
        boolean throwOnZadd = false;
        boolean closed = false;

        FakeJedis() {
            super();
        }

        @Override
        public long zadd(String key, double score, String member) {
            if (throwOnZadd) {
                throw new RuntimeException("Jedis connection error");
            }
            zaddKeys.add(key);
            zaddScores.add(score);
            zaddMembers.add(member);
            return 1L;
        }

        @Override
        public long sadd(String key, String... members) {
            if (members != null) {
                Collections.addAll(saddMembers, members);
                return members.length;
            }
            return 0L;
        }

        @Override
        public Set<String> smembers(String key) {
            return new HashSet<>(saddMembers);
        }

        @Override
        public List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
            return new ArrayList<>(readyTuples);
        }

        @Override
        public long zrem(String key, String... members) {
            if (members != null) {
                Collections.addAll(zremMembers, members);
            }
            return members != null ? members.length : 0L;
        }

        @Override
        public List<Tuple> zpopmin(String key, int count) {
            if (popTuples.isEmpty()) {
                return Collections.emptyList();
            }
            return List.of(popTuples.remove(0));
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    static class FakeJedisPool extends JedisPool {
        final FakeJedis fakeJedis;

        FakeJedisPool(FakeJedis fakeJedis) {
            super();
            this.fakeJedis = fakeJedis;
        }

        @Override
        public Jedis getResource() {
            return fakeJedis;
        }
    }

    @Test
    @DisplayName("Constructor with connection parameters initializes and shuts down cleanly")
    void testStandardConstructorAndShutdown() {
        RedisEventBroker broker = new RedisEventBroker("127.0.0.1", 6379, null, 0);
        assertNotNull(broker);

        broker.start();
        assertDoesNotThrow(() -> broker.shutdown(100));
        // Calling shutdown a second time should be a safe no-op
        assertDoesNotThrow(() -> broker.shutdown(100));
    }

    @Test
    @DisplayName("Constructor with auth password initializes cleanly")
    void testConstructorWithPassword() {
        RedisEventBroker broker = new RedisEventBroker("127.0.0.1", 6379, "secret", 2);
        assertNotNull(broker);
        broker.shutdown(50);
    }

    @Test
    @DisplayName("Publishing after broker shutdown throws IllegalStateException")
    void testPublishAfterShutdownThrows() {
        RedisEventBroker broker = new RedisEventBroker("127.0.0.1", 6379, "secret", 1);
        broker.shutdown(50);

        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "Hello Redis");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> broker.publish(msg));
        assertTrue(ex.getMessage().contains("shut down"));
    }

    @Test
    @DisplayName("Publishing to unreachable Redis throws RuntimeException")
    void testPublishUnreachableThrows() {
        JedisPool closedPool = new JedisPool("127.0.0.1", 6379);
        closedPool.close();

        RedisEventBroker broker = new RedisEventBroker(closedPool);
        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "Test");

        assertThrows(RuntimeException.class, () -> broker.publish(msg));
        broker.shutdown(50);
    }

    @Test
    @DisplayName("PublishDelayed with 0 or negative delay calls publish")
    void testPublishDelayedZeroDelay() {
        JedisPool closedPool = new JedisPool("127.0.0.1", 6379);
        closedPool.close();

        RedisEventBroker broker = new RedisEventBroker(closedPool);
        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "Test");

        assertThrows(RuntimeException.class, () -> broker.publishDelayed(msg, 0));
        assertThrows(RuntimeException.class, () -> broker.publishDelayed(msg, -100));

        broker.shutdown(50);
    }

    @Test
    @DisplayName("PublishDelayed when stopped is a no-op")
    void testPublishDelayedWhenStopped() {
        RedisEventBroker broker = new RedisEventBroker("127.0.0.1", 6379, null, 0);
        broker.shutdown(50);

        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "Test");
        assertDoesNotThrow(() -> broker.publishDelayed(msg, 5000));
    }

    @Test
    @DisplayName("Subscribe with null destination throws NullPointerException")
    void testSubscribeNullDestination() {
        RedisEventBroker broker = new RedisEventBroker("127.0.0.1", 6379, null, 0);

        assertThrows(NullPointerException.class, () ->
                broker.subscribe(null, ConsumerConfig.defaultForTopic(), String.class, m -> {}));

        broker.shutdown(50);
    }

    @Test
    @DisplayName("Duplicate subscribe on same topic throws IllegalStateException")
    void testDuplicateSubscribeThrows() {
        RedisEventBroker broker = new RedisEventBroker("127.0.0.1", 6379, null, 0);

        broker.subscribe(TestTopic.SAMPLE_TOPIC, ConsumerConfig.defaultForTopic(), String.class, m -> {});

        assertThrows(IllegalStateException.class, () ->
                broker.subscribe(TestTopic.SAMPLE_TOPIC, ConsumerConfig.defaultForTopic(), String.class, m -> {}));

        broker.shutdown(50);
    }

    @Test
    @DisplayName("Successful publish adds message to Redis ZSET with priority-calculated score and tenant key")
    void testSuccessfulPublish() {
        FakeJedis fakeJedis = new FakeJedis();
        FakeJedisPool fakePool = new FakeJedisPool(fakeJedis);

        RedisEventBroker broker = new RedisEventBroker(fakePool);

        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "payload-123", EventPriority.HIGH);
        broker.publish(msg);

        assertEquals(1, fakeJedis.zaddKeys.size());
        assertEquals("event:queue:test.redis.topic:freelance", fakeJedis.zaddKeys.get(0));
        assertTrue(fakeJedis.saddMembers.contains("freelance"));
        assertTrue(fakeJedis.closed);

        broker.shutdown(50);
    }

    @Test
    @DisplayName("Publish with explicit tenantId partitions into tenant-specific queue and updates active tenants set")
    void testMultiTenantPublish() {
        FakeJedis fakeJedis = new FakeJedis();
        FakeJedisPool fakePool = new FakeJedisPool(fakeJedis);

        RedisEventBroker broker = new RedisEventBroker(fakePool);

        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "tenant_acme", "payload-acme", EventPriority.NORMAL);
        broker.publish(msg);

        assertEquals(1, fakeJedis.zaddKeys.size());
        assertEquals("event:queue:test.redis.topic:tenant_acme", fakeJedis.zaddKeys.get(0));
        assertTrue(fakeJedis.saddMembers.contains("tenant_acme"));

        broker.shutdown(50);
    }

    @Test
    @DisplayName("Successful publishDelayed schedules item to event:delayed queue")
    void testSuccessfulPublishDelayed() {
        FakeJedis fakeJedis = new FakeJedis();
        FakeJedisPool fakePool = new FakeJedisPool(fakeJedis);

        RedisEventBroker broker = new RedisEventBroker(fakePool);

        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "delayed-payload");
        broker.publishDelayed(msg, 5000L);

        assertEquals(1, fakeJedis.zaddKeys.size());
        assertEquals("event:delayed", fakeJedis.zaddKeys.get(0));
        assertTrue(fakeJedis.closed);

        broker.shutdown(50);
    }

    @Test
    @DisplayName("publishDelayed logs error gracefully when Jedis throws exception")
    void testPublishDelayedExceptionHandled() {
        FakeJedis fakeJedis = new FakeJedis();
        fakeJedis.throwOnZadd = true;
        FakeJedisPool fakePool = new FakeJedisPool(fakeJedis);

        RedisEventBroker broker = new RedisEventBroker(fakePool);

        EventMessage<String> msg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "delayed-payload");
        assertDoesNotThrow(() -> broker.publishDelayed(msg, 5000L));

        broker.shutdown(50);
    }

    @Test
    @DisplayName("pollDelayedQueue processes ready items and republishes them")
    void testPollDelayedQueue() throws Exception {
        FakeJedis fakeJedis = new FakeJedis();
        FakeJedisPool fakePool = new FakeJedisPool(fakeJedis);

        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        EventMessage<String> originalMsg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "Delayed payload arrived");
        String json = om.writeValueAsString(originalMsg);

        fakeJedis.readyTuples.add(new Tuple(json, 12345.0));

        RedisEventBroker broker = new RedisEventBroker(fakePool);

        Method method = RedisEventBroker.class.getDeclaredMethod("pollDelayedQueue");
        method.setAccessible(true);
        method.invoke(broker);

        assertEquals(1, fakeJedis.zremMembers.size());
        assertEquals(json, fakeJedis.zremMembers.get(0));
        assertTrue(fakeJedis.zaddKeys.contains("event:queue:test.redis.topic:freelance"));

        broker.shutdown(50);
    }

    @Test
    @DisplayName("pollDelayedQueue handles corrupted JSON gracefully")
    void testPollDelayedQueueCorruptedJson() throws Exception {
        FakeJedis fakeJedis = new FakeJedis();
        FakeJedisPool fakePool = new FakeJedisPool(fakeJedis);

        String badJson = "{ invalid json syntax ";
        fakeJedis.readyTuples.add(new Tuple(badJson, 12345.0));

        RedisEventBroker broker = new RedisEventBroker(fakePool);

        Method method = RedisEventBroker.class.getDeclaredMethod("pollDelayedQueue");
        method.setAccessible(true);
        assertDoesNotThrow(() -> method.invoke(broker));

        broker.shutdown(50);
    }

    @Test
    @DisplayName("Subscriber receives message from Redis queue, runs handler, and handles shutdown")
    void testSubscriberReceivesMessage() throws Exception {
        FakeJedis fakeJedis = new FakeJedis();
        FakeJedisPool fakePool = new FakeJedisPool(fakeJedis);

        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        EventMessage<String> testMsg = EventMessage.of(TestTopic.SAMPLE_TOPIC, "Worker test payload");
        String json = om.writeValueAsString(testMsg);
        fakeJedis.popTuples.add(new Tuple(json, 100.0));

        RedisEventBroker broker = new RedisEventBroker(fakePool);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        ConsumerConfig config = new ConsumerConfig(1, 10, 1, 100, 1000);
        broker.subscribe(TestTopic.SAMPLE_TOPIC, config, String.class, msg -> {
            received.set(msg.getPayload());
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Handler should have been invoked within timeout");
        assertEquals("Worker test payload", received.get());

        broker.shutdown(500);
    }
}
