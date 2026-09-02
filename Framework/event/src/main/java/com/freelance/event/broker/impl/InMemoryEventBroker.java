package com.freelance.event.broker.impl;

import com.freelance.event.IConsumer;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.engine.GenericConsumerWorker;
import com.freelance.event.limiter.TokenBucketRateLimiter;
import com.freelance.event.model.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance, in-memory message broker backed by JVM {@link PriorityBlockingQueue} instances.
 * <p>
 * Guarantees strict priority preemption ({@code HIGH} before {@code NORMAL} before {@code LOW})
 * and FIFO ordering for equal priority tasks via monotonically increasing sequence counters.
 * </p>
 * <p>
 * When a topic is subscribed via {@link #subscribe(IConsumer, ConsumerConfig, Class, EventHandler)}, a dedicated
 * worker thread pool and rate limiter are initialized to continuously dequeue messages and delegate execution to
 * {@link com.freelance.event.engine.GenericConsumerWorker}.
 * </p>
 * <p>
 * <pre>{@code
 *  ========================================================================================
 *                             IN-MEMORY EVENT BROKER ARCHITECTURE
 *  ========================================================================================
 *
 *  [ 1. INITIALIZATION ]
 *  InMemoryEventBroker()
 *       │
 *       ├──► ConcurrentHashMap<String, TopicRegistration> topics
 *       ├──► ConcurrentHashMap<String, PriorityBlockingQueue> pendingQueues
 *       ├──► AtomicLong sequenceCounter (guarantees FIFO sequence across equal priorities)
 *       └──► ScheduledExecutorService retryScheduler (delays retries asynchronously)
 *
 *  ────────────────────────────────────────────────────────────────────────────────────────
 *
 *  [ 2. SUBSCRIPTION (Per Destination Topic) ]
 *  subscribe(IConsumer destination, ConsumerConfig config, ...)
 *       │
 *       ▼
 *  TopicRegistration(destination)
 *       ├──► Dedicated PriorityBlockingQueue<EventMessage<?>>
 *       ├──► Dedicated TokenBucketRateLimiter(rateLimitPerSecond)
 *       └──► Dedicated ExecutorService workerPool(concurrency threads)
 *                 │
 *                 ▼ (continuous worker poll loop)
 *            [ queue.poll(200ms, TimeUnit.MILLISECONDS) ]
 *                 │
 *                 ▼ (rateLimiter.acquire())
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
 *  ┌────────────────────────────────────┐                   ┌────────────────────────────────────┐
 *  │ JVM PriorityBlockingQueue:         │                   │ retryScheduler (2 daemon threads)  │
 *  │ destination topic queue            │                   │ (waits delayMillis)                │
 *  │ (Ordered: Priority -> FIFO Seq)    │                   └─────────────────┬──────────────────┘
 *  └─────────────────▲──────────────────┘                                     │
 *                    │                                                        │
 *                    │                      [ retryScheduler Timer Expired ]  ▼
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
    private final Map<String, PriorityBlockingQueue<EventMessage<?>>> pendingQueues = new ConcurrentHashMap<>();
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
            registration.queue.offer(msgToPublish);
        } else {
            pendingQueues.computeIfAbsent(destination, k -> new PriorityBlockingQueue<>()).offer(msgToPublish);
        }

        logger.debug("[InMemoryEventBroker] Published message {} to destination '{}' (priority={}, seq={})",
                msgToPublish.getId(), destination, msgToPublish.getPriority(), msgToPublish.getSequenceNumber());
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

        PriorityBlockingQueue<EventMessage<?>> pending = pendingQueues.remove(destName);
        if (pending != null && !pending.isEmpty()) {
            reg.queue.addAll(pending);
        }

        if (running.get()) {
            reg.start();
        }

        logger.info("[InMemoryEventBroker] Registered subscriber for '{}' (concurrency={}, rateLimit={}/s)",
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

        private final PriorityBlockingQueue<EventMessage<?>> queue = new PriorityBlockingQueue<>();
        private final TokenBucketRateLimiter rateLimiter;
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
            this.rateLimiter = new TokenBucketRateLimiter(this.config.rateLimitPerSecond());
            this.workerPool = Executors.newFixedThreadPool(this.config.concurrency(), r -> {
                Thread t = new Thread(r, "event-worker-" + destination + "-" + System.nanoTime() % 1000);
                t.setDaemon(true);
                return t;
            });
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
                    EventMessage<?> msg = queue.poll(200, TimeUnit.MILLISECONDS);
                    if (msg == null) {
                        continue;
                    }

                    rateLimiter.acquire();

                    GenericConsumerWorker<T> task = new GenericConsumerWorker<>(
                            msg,
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
