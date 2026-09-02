package com.freelance.event.broker.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freelance.event.IConsumer;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.engine.GenericConsumerWorker;
import com.freelance.event.limiter.TokenBucketRateLimiter;
import com.freelance.event.model.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.resps.Tuple;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * When a topic is subscribed via {@link #subscribe(IConsumer, ConsumerConfig, Class, EventHandler)}, a dedicated
 * worker thread pool and rate limiter are initialized to continuously dequeue messages and delegate execution to
 * {@link com.freelance.event.engine.GenericConsumerWorker}.
 * </p>
 * <p>
 * <pre>{@code
 *  ========================================================================================
 *                               REDIS EVENT BROKER ARCHITECTURE
 *  ========================================================================================
 *
 *  [ 1. INITIALIZATION ]
 *  RedisEventBroker(host, port, auth, db)
 *       │
 *       ├──► JedisPool (Connection Pool Configured)
 *       ├──► ObjectMapper (Jackson JSR-310 Dates)
 *       └──► ScheduledExecutorService delayedPoller (runs pollDelayedQueue every 100ms)
 *
 *  ────────────────────────────────────────────────────────────────────────────────────────
 *
 *  [ 2. SUBSCRIPTION (Per Destination Topic one queue and a worker thread to listen from that queue) ]
 *  subscribe(IConsumer destination, ConsumerConfig config, ...)
 *       │
 *       ▼
 *  RedisTopicSubscriber(destination)
 *       ├──► Dedicated TokenBucketRateLimiter(rateLimitPerSecond)
 *       └──► Dedicated ExecutorService workerPool(concurrency threads)
 *                 │
 *                 ▼ (continuous worker polling loop)
 *            [ jedis.zpopmin("event:queue:<destination>", 1) ]
 *                 │
 *                 ▼ (deserializes JSON & rateLimiter.acquire())
 *            [ GenericConsumerWorker.run() ]
 *
 *  ────────────────────────────────────────────────────────────────────────────────────────
 *
 *  [ 3. PUBLISHING & DELAYED RETRY FLOW ]
 *
 *     [ Standard Event ]                                     [ Delayed / Retry Event ]
 *   EventBroker.publish(msg)                             EventBroker.publishDelayed(msg, delay)
 *            │                                                        │
 *            │ (score = priority * 1e13 + timestamp)                  │ (score = now + delayMillis)
 *            ▼                                                        ▼
 *  ┌────────────────────────────────────┐                   ┌────────────────────────────────────┐
 *  │ Redis ZSET:                        │                   │ Redis ZSET:                        │
 *  │ "event:queue:<destination>"        │                   │ "event:delayed"                    │
 *  │ (Ordered: Priority -> FIFO Time)   │                   │ (Ordered by trigger timestamp)     │
 *  └─────────────────▲──────────────────┘                   └─────────────────┬──────────────────┘
 *                    │                                                        │
 *                    │                      [ delayedPoller Executor ]        │
 *                    │               (polls every 100ms: score <= now)        ▼
 *                    └─────────────────────── jedis.zrangeByScore("event:delayed", 0, now)
 *                                             jedis.zrem(...)
 *                                             broker.publish(readyMsg)
 *
 *  ========================================================================================
 * }</pre>
 * </p>
 *
 * @see com.freelance.event.engine.GenericConsumerWorker
 * @see EventBroker
 */
public class RedisEventBroker implements EventBroker {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventBroker.class);
    private static final String QUEUE_PREFIX = "event:queue:";
    private static final String DELAYED_QUEUE_KEY = "event:delayed";
    private static final double PRIORITY_MULTIPLIER = 10_000_000_000_000.0;

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final Map<String, RedisTopicSubscriber<?>> subscribers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService delayedPoller = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "redis-delayed-poller");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(true);

    public RedisEventBroker(String host, int port, String password, int database) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(64);
        poolConfig.setMaxIdle(16);
        poolConfig.setMinIdle(4);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(1));

        String authPass = (password != null && !password.isBlank()) ? password : null;
        this.jedisPool = new JedisPool(poolConfig, host, port, 3000, authPass, database);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.delayedPoller.scheduleWithFixedDelay(this::pollDelayedQueue, 100, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> void publish(EventMessage<T> message) {
        if (!running.get()) {
            throw new IllegalStateException("Cannot publish event; RedisEventBroker has been shut down.");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String jsonPayload = objectMapper.writeValueAsString(message);
            double score = (message.getPriority().getLevel() * PRIORITY_MULTIPLIER) + message.getTimestamp().toEpochMilli();
            String queueKey = QUEUE_PREFIX + message.getDestination();

            jedis.zadd(queueKey, score, jsonPayload);

            logger.debug("[RedisEventBroker] Published message {} to Redis key '{}' (priority={}, score={})",
                    message.getId(), queueKey, message.getPriority(), score);
        } catch (Exception e) {
            logger.error("[RedisEventBroker] Failed to publish message {} to destination '{}'",
                    message.getId(), message.getDestination(), e);
            throw new RuntimeException("Redis event publication failed", e);
        }
    }

    @Override
    public <T> void publishDelayed(EventMessage<T> message, long delayMillis) {
        if (!running.get()) return;

        if (delayMillis <= 0) {
            publish(message);
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String jsonPayload = objectMapper.writeValueAsString(message);
            double triggerTimestamp = System.currentTimeMillis() + delayMillis;

            jedis.zadd(DELAYED_QUEUE_KEY, triggerTimestamp, jsonPayload);

            logger.debug("[RedisEventBroker] Scheduled delayed retry for message {} in {}ms (triggerAt={})",
                    message.getId(), delayMillis, triggerTimestamp);
        } catch (Exception e) {
            logger.error("[RedisEventBroker] Failed to schedule delayed message {}", message.getId(), e);
        }
    }

    private void pollDelayedQueue() {
        if (!running.get()) return;

        try (Jedis jedis = jedisPool.getResource()) {
            long now = System.currentTimeMillis();
            List<Tuple> readyItems = jedis.zrangeByScoreWithScores(DELAYED_QUEUE_KEY, 0, now, 0, 50);

            for (Tuple tuple : readyItems) {
                String json = tuple.getElement();
                long removed = jedis.zrem(DELAYED_QUEUE_KEY, json);
                if (removed > 0) {
                    try {
                        EventMessage<?> msg = objectMapper.readValue(json, EventMessage.class);
                        publish(msg);
                    } catch (Exception parseEx) {
                        logger.error("[RedisEventBroker] Failed to parse delayed event JSON", parseEx);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("[RedisEventBroker] Delayed queue poll iteration skipped: {}", e.getMessage());
        }
    }

    @Override
    public <T> void subscribe(
            IConsumer destination,
            ConsumerConfig config,
            Class<T> payloadClass,
            EventHandler<T> handler
    ) {
        Objects.requireNonNull(destination, "destination must not be null");
        String destName = destination.getDestination();

        RedisTopicSubscriber<T> subscriber = new RedisTopicSubscriber<>(
                destName,
                config,
                payloadClass,
                handler,
                this,
                jedisPool,
                objectMapper
        );

        RedisTopicSubscriber<?> existing = subscribers.putIfAbsent(destName, subscriber);
        if (existing != null) {
            throw new IllegalStateException("Redis consumer already registered for destination '" + destName + "'.");
        }

        if (running.get()) {
            subscriber.start();
        }

        logger.info("[RedisEventBroker] Registered Redis consumer for '{}' (concurrency={}, rateLimit={}/s)",
                destName, config.concurrency(), config.rateLimitPerSecond());
    }

    @Override
    public void start() {
        running.set(true);
        subscribers.values().forEach(RedisTopicSubscriber::start);
    }

    @Override
    public void shutdown(long timeoutMillis) {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        logger.info("[RedisEventBroker] Initiating graceful shutdown (timeout={}ms)...", timeoutMillis);

        delayedPoller.shutdown();
        long deadline = System.currentTimeMillis() + timeoutMillis;

        for (RedisTopicSubscriber<?> sub : subscribers.values()) {
            sub.stop(Math.max(100, deadline - System.currentTimeMillis()));
        }

        try {
            if (!delayedPoller.awaitTermination(Math.max(100, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS)) {
                delayedPoller.shutdownNow();
            }
        } catch (InterruptedException e) {
            delayedPoller.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }

        logger.info("[RedisEventBroker] Graceful shutdown completed.");
    }

    private static class RedisTopicSubscriber<T> {
        private final String destination;
        private final ConsumerConfig config;
        private final Class<T> payloadClass;
        private final EventHandler<T> handler;
        private final EventBroker broker;
        private final JedisPool jedisPool;
        private final ObjectMapper objectMapper;

        private final TokenBucketRateLimiter rateLimiter;
        private final ExecutorService workerPool;
        private final AtomicBoolean active = new AtomicBoolean(false);

        RedisTopicSubscriber(
                String destination,
                ConsumerConfig config,
                Class<T> payloadClass,
                EventHandler<T> handler,
                EventBroker broker,
                JedisPool jedisPool,
                ObjectMapper objectMapper
        ) {
            this.destination = destination;
            this.config = config != null ? config : ConsumerConfig.defaultForTopic();
            this.payloadClass = payloadClass;
            this.handler = handler;
            this.broker = broker;
            this.jedisPool = jedisPool;
            this.objectMapper = objectMapper;
            this.rateLimiter = new TokenBucketRateLimiter(this.config.rateLimitPerSecond());
            this.workerPool = Executors.newFixedThreadPool(this.config.concurrency(), r -> {
                Thread t = new Thread(r, "redis-worker-" + destination + "-" + System.nanoTime() % 1000);
                t.setDaemon(true);
                return t;
            });
        }

        void start() {
            if (active.compareAndSet(false, true)) {
                for (int i = 0; i < config.concurrency(); i++) {
                    workerPool.submit(this::pollAndProcessLoop);
                }
            }
        }

        private void pollAndProcessLoop() {
            String queueKey = QUEUE_PREFIX + destination;

            while (active.get()) {
                try {
                    List<Tuple> items;
                    try (Jedis jedis = jedisPool.getResource()) {
                        items = jedis.zpopmin(queueKey, 1);
                    }

                    if (items == null || items.isEmpty()) {
                        Thread.sleep(50);
                        continue;
                    }

                    String json = items.getFirst().getElement();
                    EventMessage<?> rawMessage = objectMapper.readValue(json, EventMessage.class);

                    rateLimiter.acquire();

                    GenericConsumerWorker<T> task = new GenericConsumerWorker<>(
                            rawMessage,
                            payloadClass,
                            handler,
                            config,
                            broker
                    );

                    task.run();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.debug("[RedisEventBroker] Worker poll loop error on '{}': {}", destination, e.getMessage());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        void stop(long waitMillis) {
            active.set(false);
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(waitMillis, TimeUnit.MILLISECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
