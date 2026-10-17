package com.freelance.event.broker.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freelance.ConfigurationEngine;
import com.freelance.config.IConfig;
import com.freelance.event.IConsumer;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.engine.GenericConsumerWorker;
import com.freelance.event.limiter.TokenBucketRateLimiter;
import com.freelance.event.model.EventMessage;
import com.freelance.registry.ConfigurationRegistry;
import com.freelance.system.SystemHardcoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.resps.Tuple;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-throughput, distributed Redis-backed multi-tenant event broker.
 * <p>
 * Implements tenant-isolated queue partitioning ({@code event:queue:<destination>:<tenantId>}), tenant tracking sets
 * ({@code event:tenants:<destination>}), fair-share round-robin dispatching across active tenants to prevent starvation,
 * per-tenant {@link TokenBucketRateLimiter} instances, priority preemption via Redis ZSET composite scores
 * ({@code priority * 10^13 + timestamp}), and atomic message dequeuing via {@code ZPOPMIN}.
 * </p>
 * <p>
 * Delayed retries and scheduled message republishing are handled via a global sorted set ({@code event:delayed})
 * continuously polled by a dedicated daemon executor.
 * </p>
 * <p>
 * <pre>{@code
 *  ========================================================================================
 *                             REDIS MULTI-TENANT BROKER ARCHITECTURE
 *  ========================================================================================
 *
 *  [ 1. INITIALIZATION ]
 *  RedisEventBroker(host, port, auth, db)
 *       │
 *       ├──► JedisPool (Configured connection pooling)
 *       ├──► ObjectMapper (Jackson JSR-310 JavaTimeModule)
 *       └──► ScheduledExecutorService delayedPoller (polls "event:delayed" every 100ms)
 *
 *  ────────────────────────────────────────────────────────────────────────────────────────
 *
 *  [ 2. SUBSCRIPTION & FAIR-SHARE DISPATCH (Per Destination Topic) ]
 *  subscribe(IConsumer destination, ConsumerConfig config, ...)
 *       │
 *       ▼
 *  RedisTopicSubscriber(destination)
 *       ├──► ConcurrentHashMap<String, TokenBucketRateLimiter> rateLimiters (Per-Tenant Limiters)
 *       ├──► AtomicInteger roundRobinCounter (Fair-Share Tenant Scheduling)
 *       └──► Dedicated ExecutorService workerPool(concurrency threads)
 *                 │
 *                 ▼ (fair-share round-robin across active tenant queues)
 *            [ jedis.smembers("event:tenants:<destination>") ]
 *            [ jedis.zpopmin("event:queue:<destination>:<tenantId>", 1) ]
 *                 │
 *                 ▼ (deserializes JSON & per-tenant rateLimiter.acquire())
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
 *  ┌──────────────────────────────────────────────┐          ┌────────────────────────────────────┐
 *  │ Redis ZSET:                                  │          │ Redis ZSET:                        │
 *  │ "event:queue:<destination>:<tenantId>"       │          │ "event:delayed"                    │
 *  │ Redis SET:                                   │          │ (Ordered by trigger timestamp)     │
 *  │ "event:tenants:<destination>"                │          └─────────────────┬──────────────────┘
 *  │ (Ordered: Priority -> FIFO Time)             │                            │
 *  └─────────────────▲────────────────────────────┘                            │
 *                    │                                                         │
 *                    │                               [ delayedPoller ]         │
 *                    │                      (polls every 100ms: score <= now)  ▼
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
    private static final String TENANTS_PREFIX = "event:tenants:";
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
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(32);
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

    public RedisEventBroker(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
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
            String tenantId = (message.getTenantId() != null && !message.getTenantId().isBlank())
                    ? message.getTenantId()
                    : SystemHardcoding.DEFAULT_TENANT.asString();
            String queueKey = QUEUE_PREFIX + message.getDestination() + ":" + tenantId;
            String tenantsSetKey = TENANTS_PREFIX + message.getDestination();

            jedis.zadd(queueKey, score, jsonPayload);
            jedis.sadd(tenantsSetKey, tenantId);

            logger.debug("[RedisEventBroker] Published message {} to Redis key '{}' for tenant '{}' (priority={}, score={})",
                    message.getId(), queueKey, tenantId, message.getPriority(), score);
        } catch (Exception e) {
            logger.error("[RedisEventBroker] Failed to publish message {} to destination '{}'",
                    message.getId(), message.getDestination(), e);
            throw new RuntimeException("Redis event publication failed", e);
        }
    }

    @Override
    public <T> void publishBatch(List<EventMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        if (!running.get()) {
            throw new IllegalStateException("Cannot publish event; RedisEventBroker has been shut down.");
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            for (EventMessage<T> message : messages) {
                String jsonPayload = objectMapper.writeValueAsString(message);
                double score = (message.getPriority().getLevel() * PRIORITY_MULTIPLIER) + message.getTimestamp().toEpochMilli();
                String tenantId = (message.getTenantId() != null && !message.getTenantId().isBlank())
                        ? message.getTenantId()
                        : SystemHardcoding.DEFAULT_TENANT.asString();
                String queueKey = QUEUE_PREFIX + message.getDestination() + ":" + tenantId;
                String tenantsSetKey = TENANTS_PREFIX + message.getDestination();

                pipeline.zadd(queueKey, score, jsonPayload);
                pipeline.sadd(tenantsSetKey, tenantId);
            }
            pipeline.sync();

            logger.debug("[RedisEventBroker] Batch published {} messages to Redis", messages.size());
        } catch (Exception e) {
            logger.error("[RedisEventBroker] Failed to batch publish {} messages", messages.size(), e);
            throw new RuntimeException("Redis event batch publication failed", e);
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

        logger.info("[RedisEventBroker] Registered multi-tenant Redis consumer for '{}' (concurrency={}, rateLimit={}/s)",
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

        private final ConcurrentHashMap<String, TokenBucketRateLimiter> rateLimiters = new ConcurrentHashMap<>();
        private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
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
            this.workerPool = Executors.newFixedThreadPool(this.config.concurrency(), r -> {
                Thread t = new Thread(r, "redis-worker-" + destination + "-" + System.nanoTime() % 1000);
                t.setDaemon(true);
                return t;
            });
        }

        private double resolveRateLimitPerSecond(String tenantId) {
            String configKey = "event.consumer." + destination + ".rate_limit";
            IConfig registered = ConfigurationRegistry.getConfig(configKey);
            if (registered != null) {
                return ConfigurationEngine.getDouble(registered, config.rateLimitPerSecond(), tenantId);
            }
            return config.rateLimitPerSecond();
        }

        private TokenBucketRateLimiter getOrCreateRateLimiter(String tenantId) {
            double effectiveRate = resolveRateLimitPerSecond(tenantId);
            TokenBucketRateLimiter limiter = rateLimiters.computeIfAbsent(tenantId, t -> new TokenBucketRateLimiter(effectiveRate));
            if (limiter.getRatePerSecond() != effectiveRate) {
                limiter.setRatePerSecond(effectiveRate);
            }
            return limiter;
        }

        void start() {
            if (active.compareAndSet(false, true)) {
                for (int i = 0; i < config.concurrency(); i++) {
                    workerPool.submit(this::pollAndProcessLoop);
                }
            }
        }

        private void pollAndProcessLoop() {
            String tenantsSetKey = TENANTS_PREFIX + destination;

            while (active.get()) {
                try {
                    Set<String> tenants;
                    try (Jedis jedis = jedisPool.getResource()) {
                        tenants = jedis.smembers(tenantsSetKey);
                    }

                    if (tenants == null || tenants.isEmpty()) {
                        tenants = Set.of(SystemHardcoding.DEFAULT_TENANT.asString());
                    }

                    List<String> tenantList = new ArrayList<>(tenants);
                    if (tenantList.isEmpty()) {
                        Thread.sleep(50);
                        continue;
                    }

                    int tenantCount = tenantList.size();
                    int startIdx = (roundRobinCounter.getAndIncrement() & 0x7fffffff) % tenantCount;
                    boolean processed = false;

                    for (int i = 0; i < tenantCount; i++) {
                        int currentIdx = (startIdx + i) % tenantCount;
                        if (currentIdx >= tenantList.size()) {
                            break;
                        }
                        String currentTenant = tenantList.get(currentIdx);
                        String queueKey = QUEUE_PREFIX + destination + ":" + currentTenant;

                        List<Tuple> items;
                        try (Jedis jedis = jedisPool.getResource()) {
                            items = jedis.zpopmin(queueKey, 1);
                        }

                        if (items != null && !items.isEmpty()) {
                            String json = items.getFirst().getElement();
                            EventMessage<?> rawMessage = objectMapper.readValue(json, EventMessage.class);

                            TokenBucketRateLimiter limiter = getOrCreateRateLimiter(rawMessage.getTenantId());
                            limiter.acquire();

                            GenericConsumerWorker<T> task = new GenericConsumerWorker<>(
                                    rawMessage,
                                    payloadClass,
                                    handler,
                                    config,
                                    broker
                            );

                            task.run();
                            processed = true;
                            break;
                        }
                    }

                    if (!processed) {
                        Thread.sleep(50);
                    }

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
