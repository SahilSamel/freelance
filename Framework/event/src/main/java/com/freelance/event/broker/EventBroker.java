package com.freelance.event.broker;

import com.freelance.event.IConsumer;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.engine.GenericConsumerWorker;
import com.freelance.event.model.EventMessage;

import java.util.List;

/**
 * Pluggable message transport and storage driver interface for the asynchronous event management framework.
 * <p>
 * <b>Why implement this interface?</b><br>
 * {@code EventBroker} serves as the core transport abstraction separating event producers (calls to
 * {@code EventManagementEngine.emit}) and consumers (classes implementing {@link EventHandler}) from the underlying
 * message delivery infrastructure. By implementing this interface, custom transport backends (such as JVM In-Memory
 * queues, Redis Sorted Sets, RabbitMQ, Apache Kafka, or AWS SQS) can be plugged into the framework seamlessly without
 * altering application-level business logic.
 * </p>
 * <p>
 * <b>Core Implementation Requirements:</b>
 * <ul>
 *   <li><b>Priority Preemption:</b> The broker backend <b>MUST</b> guarantee delivery ordered by
 *       {@link EventMessage#getPriority()} (e.g., {@code HIGH} priority tasks execute before {@code NORMAL} or {@code LOW}).</li>
 *   <li><b>FIFO Ordering:</b> Events sharing the same priority level must be delivered in strict FIFO sequence using
 *       {@link EventMessage#getSequenceNumber()} and {@link EventMessage#getTimestamp()}.</li>
 *   <li><b>Worker Isolation:</b> Each subscribed destination must operate its own dedicated worker thread pool conforming to
 *       {@link ConsumerConfig#concurrency()} and apply throughput throttling via {@link ConsumerConfig#rateLimitPerSecond()}.</li>
 *   <li><b>Task Execution:</b> Worker threads must delegate actual task execution to {@link GenericConsumerWorker}
 *       to ensure thread-local JPA transactions, MDC logging trace IDs, caller user contexts, retry backoffs, and dead-letter
 *       handling are correctly established.</li>
 *   <li><b>Broker Driver Registration:</b> Any new implementation of {@code EventBroker} (e.g. RabbitMQ, Kafka, SQS)
 *       <b>MUST</b> register a corresponding enum constant in {@link EventBrokerType} and configure its creation in
 *       {@link EventBrokerFactory}.</li>
 * </ul>
 * </p>
 *
 * @see EventBrokerType
 * @see EventBrokerFactory
 */
public interface EventBroker {

    /**
     * Publishes an event message envelope to its destination topic immediately.
     * <p>
     * <ul>
     *   <li>Must be thread-safe and non-blocking, returning in sub-millisecond time.</li>
     *   <li>Must persist or enqueue the event into the queue corresponding to {@link EventMessage#getDestination()}.</li>
     *   <li>Must assign a monotonic sequence number if {@link EventMessage#getSequenceNumber()} is unassigned (0) to preserve FIFO ordering.</li>
     *   <li>Must maintain priority order such that higher-priority messages are positioned ahead of lower-priority ones.</li>
     * </ul>
     * </p>
     *
     * @param <T>     the payload type
     * @param message the event message envelope containing payload and metadata; must not be null
     * @throws IllegalStateException if the broker has been shut down
     */
    <T> void publish(EventMessage<T> message);

    /**
     * Publishes a collection of event message envelopes to their destination topics.
     * <p>
     * Enables underlying broker drivers to apply batch transport optimizations such as network pipelining.
     * </p>
     *
     * @param <T>      the payload type
     * @param messages collection of event message envelopes; must not be null
     * @throws IllegalStateException if the broker has been shut down
     */
    default <T> void publishBatch(List<EventMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (EventMessage<T> message : messages) {
            publish(message);
        }
    }

    /**
     * Publishes an event message envelope with a scheduled delivery delay.
     * <p>
     * <ul>
     *   <li>Used primarily by retry mechanisms during exponential backoff.</li>
     *   <li>Must withhold the message from worker consumption until {@code delayMillis} has elapsed.</li>
     *   <li>If {@code delayMillis <= 0}, must delegate immediately to {@link #publish(EventMessage)}.</li>
     *   <li>Once the delay expires, the message must become eligible for consumption following standard priority rules.</li>
     * </ul>
     * </p>
     *
     * @param <T>         the payload type
     * @param message     the event message envelope to publish; must not be null
     * @param delayMillis delay in milliseconds before the message becomes eligible for consumption
     */
    <T> void publishDelayed(EventMessage<T> message, long delayMillis);

    /**
     * Subscribes a consumer handler to a destination topic defined by an {@link IConsumer}.
     * <p>
     * <ul>
     *   <li>Must initialize a dedicated worker thread pool sized according to {@link ConsumerConfig#concurrency()}.</li>
     *   <li>Must instantiate and apply throughput rate limiting according to {@link ConsumerConfig#rateLimitPerSecond()} (e.g. using Token Bucket).</li>
     *   <li>Must prevent duplicate consumer registrations for the same destination topic (e.g. throw {@link IllegalStateException}).</li>
     *   <li>Worker threads must continuously poll messages in priority order, deserialize the payload into {@code payloadClass},
     *       and execute {@link GenericConsumerWorker}.</li>
     *   <li>If the broker is already active when subscribed, the consumer worker threads must start polling immediately.</li>
     * </ul>
     * </p>
     *
     * @param <T>          the payload type
     * @param destination  the unique destination topic interface/enum
     * @param config       the consumer throttling, retry, and concurrency configuration
     * @param payloadClass the target DTO class for payload deserialization
     * @param handler      the functional handler processing incoming tasks
     */
    <T> void subscribe(
            IConsumer destination,
            ConsumerConfig config,
            Class<T> payloadClass,
            EventHandler<T> handler
    );

    /**
     * Initializes and starts broker worker threads, pollers, and scheduled executors.
     * <p>
     * <ul>
     *   <li>Transitions the broker into the active running state.</li>
     *   <li>Starts all registered consumer worker thread pools and background delayed-message pollers.</li>
     *   <li>Must be safe to invoke multiple times (idempotent).</li>
     * </ul>
     * </p>
     */
    void start();

    /**
     * Initiates a graceful shutdown of the broker and its worker thread pools.
     * <p>
     * <ul>
     *   <li>Must stop accepting new incoming events immediately.</li>
     *   <li>Must shutdown delayed retry schedulers and stop poller threads.</li>
     *   <li>Must signal worker thread pools to finish in-flight tasks and await termination up to {@code timeoutMillis}.</li>
     *   <li>If the deadline expires with tasks still in-flight, forcefully terminates remaining workers and releases underlying resources.</li>
     * </ul>
     * </p>
     *
     * @param timeoutMillis maximum duration in milliseconds to wait for in-flight tasks to finish
     */
    void shutdown(long timeoutMillis);
}
