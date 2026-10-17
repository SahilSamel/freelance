package com.freelance.event.broker.impl;

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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance, in-memory multi-tenant event broker backed by isolated JVM {@link PriorityBlockingQueue} instances.
 * <p>
 * Provides tenant isolation per destination topic, fair-share round-robin scheduling across active tenants to prevent
 * noisy-neighbor starvation, per-tenant {@link TokenBucketRateLimiter} instances, strict priority preemption
 * ({@code HIGH} before {@code NORMAL} before {@code LOW}), and FIFO ordering for equal priority tasks via monotonically
 * increasing sequence counters.
 * </p>
 * <p>
 * When a topic is subscribed via {@link #subscribe(IConsumer, ConsumerConfig, Class, EventHandler)}, a dedicated
 * worker thread pool is initialized to continuously dequeue messages across active tenant queues using round-robin
 * scheduling and delegate execution to {@link com.freelance.event.engine.GenericConsumerWorker}.
 * </p>
 * <p>
 * <pre>{@code
 *  ========================================================================================
 *                         IN-MEMORY MULTI-TENANT BROKER ARCHITECTURE
 *  ========================================================================================
 *
 *  [ 1. INITIALIZATION ]
 *  InMemoryEventBroker()
 *       │
 *       ├──► ConcurrentHashMap<String, TopicRegistration<?>> topics
 *       ├──► ConcurrentHashMap<String, ConcurrentLinkedQueue<EventMessage<?>>> pendingQueues
 *       ├──► AtomicLong sequenceCounter (guarantees FIFO sequence across equal priorities)
 *       └──► ScheduledExecutorService retryScheduler (delays retries asynchronously)
 *
 *  ────────────────────────────────────────────────────────────────────────────────────────
 *
 *  [ 2. SUBSCRIPTION & MULTI-TENANT DISPATCH (Per Destination Topic) ]
 *  subscribe(IConsumer destination, ConsumerConfig config, ...)
 *       │
 *       ▼
 *  TopicRegistration(destination)
 *       ├──► ConcurrentHashMap<String, PriorityBlockingQueue<EventMessage<?>>> tenantQueues
 *       ├──► ConcurrentHashMap<String, TokenBucketRateLimiter> rateLimiters (Per-Tenant Limiters)
 *       ├──► List<String> activeTenants & AtomicInteger roundRobinCounter (Fair-Share Round-Robin)
 *       └──► Dedicated ExecutorService workerPool(concurrency threads)
 *                 │
 *                 ▼ (continuous fair round-robin poll across active tenants)
 *            [ tenantQueues.get(tenantId).poll() ]
 *                 │
 *                 ▼ (per-tenant rateLimiter.acquire())
 *            [ GenericConsumerWorker.run() ]
 *
 *  ────────────────────────────────────────────────────────────────────────────────────────
 *
 *  [ 3. PUBLISHING & DELAYED RETRY FLOW ]
 *
 *     [ Standard Event ]                                     [ Delayed / Retry Event ]
 *   EventBroker.publish(msg)                             EventBroker.publishDelayed(msg, delay)
 *            │                                                        │
 *            │ (assigns monotonic sequence counter)                   │ (schedules timer task)
 *            ▼                                                        ▼
 *  ┌──────────────────────────────────────────────┐          ┌────────────────────────────────────┐
 *  │ TopicRegistration.enqueue(msg):              │          │ retryScheduler (2 daemon threads)  │
 *  │ tenantQueues.get(tenantId)                   │          │ (waits delayMillis)                │
 *  │ (Ordered: Priority -> FIFO Seq)              │          └─────────────────┬──────────────────┘
 *  │ Adds tenantId to activeTenants               │                            │
 *  └─────────────────▲────────────────────────────┘                            │
 *                    │                                                         │
 *                    │                      [ retryScheduler Timer Expired ]   ▼
 *                    └─────────────────────── broker.publish(retryMsg)
 *
 *  ========================================================================================
 * }</pre>
 * </p>
 *
 * @see com.freelance.event.engine.GenericConsumerWorker
 * @see EventBroker
 */
public class InMemoryEventBroker implements EventBroker {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryEventBroker.class);

    private final Map<String, TopicRegistration<?>> topics = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<EventMessage<?>>> pendingQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "event-retry-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public <T> void publish(EventMessage<T> message) {
        if (!running.get()) {
            throw new IllegalStateException("Cannot publish event; InMemoryEventBroker has been shut down.");
        }

        EventMessage<T> msgToPublish = (message.getSequenceNumber() == 0)
                ? message.withSequenceNumber(sequenceCounter.incrementAndGet())
                : message;

        String destination = msgToPublish.getDestination();
        TopicRegistration<?> registration = topics.get(destination);

        if (registration != null) {
            registration.enqueue(msgToPublish);
        } else {
            pendingQueues.computeIfAbsent(destination, k -> new ConcurrentLinkedQueue<>()).offer(msgToPublish);
        }

        logger.debug("[InMemoryEventBroker] Published message {} to destination '{}' for tenant '{}' (priority={}, seq={})",
                msgToPublish.getId(), destination, msgToPublish.getTenantId(), msgToPublish.getPriority(), msgToPublish.getSequenceNumber());
    }

    @Override
    public <T> void publishBatch(List<EventMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) return;
        for (EventMessage<T> msg : messages) {
            publish(msg);
        }
    }

    @Override
    public <T> void publishDelayed(EventMessage<T> message, long delayMillis) {
        if (!running.get()) return;

        if (delayMillis <= 0) {
            publish(message);
            return;
        }

        retryScheduler.schedule(() -> {
            try {
                publish(message);
            } catch (Exception e) {
                logger.error("[InMemoryEventBroker] Failed to republish delayed message {}", message.getId(), e);
            }
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(
            IConsumer destination,
            ConsumerConfig config,
            Class<T> payloadClass,
            EventHandler<T> handler
    ) {
        Objects.requireNonNull(destination, "destination must not be null");
        String destName = destination.getDestination();

        TopicRegistration<T> reg = new TopicRegistration<>(
                destName,
                config,
                payloadClass,
                handler,
                this
        );

        TopicRegistration<?> existing = topics.putIfAbsent(destName, reg);
        if (existing != null) {
            throw new IllegalStateException("Consumer already registered for destination topic '" + destName + "'.");
        }

        ConcurrentLinkedQueue<EventMessage<?>> pending = pendingQueues.remove(destName);
        if (pending != null) {
            EventMessage<?> pendingMsg;
            while ((pendingMsg = pending.poll()) != null) {
                reg.enqueue(pendingMsg);
            }
        }

        if (running.get()) {
            reg.start();
        }

        logger.info("[InMemoryEventBroker] Registered multi-tenant subscriber for '{}' (concurrency={}, rateLimit={}/s)",
                destName, config.concurrency(), config.rateLimitPerSecond());
    }

    @Override
    public void start() {
        running.set(true);
        topics.values().forEach(TopicRegistration::start);
    }

    @Override
    public void shutdown(long timeoutMillis) {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        logger.info("[InMemoryEventBroker] Initiating graceful shutdown (timeout={}ms)...", timeoutMillis);

        retryScheduler.shutdown();
        long deadline = System.currentTimeMillis() + timeoutMillis;

        for (TopicRegistration<?> reg : topics.values()) {
            reg.stop(Math.max(100, deadline - System.currentTimeMillis()));
        }

        try {
            if (!retryScheduler.awaitTermination(Math.max(100, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS)) {
                retryScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("[InMemoryEventBroker] Graceful shutdown completed.");
    }

    private static class TopicRegistration<T> {
        private final String destination;
        private final ConsumerConfig config;
        private final Class<T> payloadClass;
        private final EventHandler<T> handler;
        private final EventBroker broker;

        private final ConcurrentHashMap<String, PriorityBlockingQueue<EventMessage<?>>> tenantQueues = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, TokenBucketRateLimiter> rateLimiters = new ConcurrentHashMap<>();
        private final List<String> activeTenants = new CopyOnWriteArrayList<>();
        private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

        private final ExecutorService workerPool;
        private final AtomicBoolean active = new AtomicBoolean(false);

        TopicRegistration(
                String destination,
                ConsumerConfig config,
                Class<T> payloadClass,
                EventHandler<T> handler,
                EventBroker broker
        ) {
            this.destination = destination;
            this.config = config != null ? config : ConsumerConfig.defaultForTopic();
            this.payloadClass = payloadClass;
            this.handler = handler;
            this.broker = broker;
            this.workerPool = Executors.newFixedThreadPool(this.config.concurrency(), r -> {
                Thread t = new Thread(r, "event-worker-" + destination + "-" + System.nanoTime() % 1000);
                t.setDaemon(true);
                return t;
            });
        }

        void enqueue(EventMessage<?> msg) {
            String tenantId = (msg.getTenantId() != null && !msg.getTenantId().isBlank()) ? msg.getTenantId() : SystemHardcoding.DEFAULT_TENANT.asString();
            tenantQueues.computeIfAbsent(tenantId, t -> {
                if (!activeTenants.contains(t)) {
                    activeTenants.add(t);
                }
                return new PriorityBlockingQueue<>();
            }).offer(msg);
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
                    workerPool.submit(this::processLoop);
                }
            }
        }

        private void processLoop() {
            while (active.get()) {
                try {
                    if (activeTenants.isEmpty()) {
                        Thread.sleep(50);
                        continue;
                    }

                    int tenantCount = activeTenants.size();
                    int startIdx = (roundRobinCounter.getAndIncrement() & 0x7fffffff) % tenantCount;
                    boolean processed = false;

                    for (int i = 0; i < tenantCount; i++) {
                        int currentIdx = (startIdx + i) % tenantCount;
                        if (currentIdx >= activeTenants.size()) {
                            break;
                        }
                        String tenantId = activeTenants.get(currentIdx);
                        PriorityBlockingQueue<EventMessage<?>> q = tenantQueues.get(tenantId);
                        if (q != null) {
                            EventMessage<?> msg = q.poll();
                            if (msg != null) {
                                TokenBucketRateLimiter limiter = getOrCreateRateLimiter(msg.getTenantId());
                                limiter.acquire();

                                GenericConsumerWorker<T> task = new GenericConsumerWorker<>(
                                        msg,
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
                    }

                    if (!processed) {
                        Thread.sleep(50);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("[InMemoryEventBroker] Unexpected error in worker loop for '{}'", destination, e);
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
