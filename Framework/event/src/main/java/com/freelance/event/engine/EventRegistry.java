package com.freelance.event.engine;

import com.freelance.ConfigurationEngine;
import com.freelance.event.IConsumer;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.broker.EventBrokerFactory;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.keys.event.EventConfigKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Thread-safe registry responsible for managing the lifecycle, discovery,
 * and consumer subscriptions of the {@link EventBroker}.
 */
public class EventRegistry {

    private static final Logger logger = LoggerFactory.getLogger(EventRegistry.class);

    private static final class Holder {
        private static final EventRegistry INSTANCE = new EventRegistry();
    }

    public static EventRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private final EventConsumerScanner scanner = new EventConsumerScanner();
    private volatile EventBroker broker;
    private volatile boolean initialized = false;

    public synchronized void init() {
        if (initialized) {
            return;
        }

        if (this.broker == null) {
            this.broker = EventBrokerFactory.createBroker();
        }

        scanner.scanAndRegister(this.broker);
        this.broker.start();
        this.initialized = true;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            int timeoutSec = ConfigurationEngine.getInt(EventConfigKeys.EVENT_SHUTDOWN_TIMEOUT_SECONDS);
            shutdown(timeoutSec * 1000L);
        }, "event-engine-shutdown-hook"));

        logger.info("[EventRegistry] Event management framework initialized successfully.");
    }

    public synchronized void shutdown(long timeoutMillis) {
        if (this.broker != null) {
            this.broker.shutdown(timeoutMillis);
        }
        this.initialized = false;
    }

    public <T> void registerConsumer(
            IConsumer destination,
            ConsumerConfig config,
            Class<T> payloadClass,
            EventHandler<T> handler
    ) {
        Objects.requireNonNull(destination, "destination must not be null");
        getOrCreateBroker().subscribe(destination, config, payloadClass, handler);
    }

    public EventBroker getOrCreateBroker() {
        if (this.broker == null) {
            synchronized (this) {
                if (this.broker == null) {
                    this.broker = EventBrokerFactory.createBroker();
                    this.broker.start();
                }
            }
        }
        return this.broker;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public synchronized void setBrokerForTesting(EventBroker broker) {
        if (this.broker != null && this.broker != broker) {
            this.broker.shutdown(500);
        }
        this.broker = broker;
        this.initialized = (broker != null);
    }
}
