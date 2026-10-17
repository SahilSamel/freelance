package com.freelance.event.engine;

import com.freelance.event.IConsumer;
import com.freelance.event.broker.EventBroker;
import com.freelance.event.consumer.ConsumerConfig;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventConsumer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Discovers classes annotated with {@link EventConsumer} on the classpath, instantiates them,
 * and automatically subscribes them to their declared topic with the active {@link EventBroker}.
 * <p>
 * Because the broker is already running, registering a consumer immediately starts its dedicated
 * background worker thread pool to begin listening and executing incoming events for that topic.
 * </p>
 */
public class EventConsumerScanner {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumerScanner.class);
    public static final String MANDATED_CONSUMER_PACKAGE = "com.freelance.consumer";

    public int scanAndRegister(EventBroker broker) {
        String packageToScan = MANDATED_CONSUMER_PACKAGE;

        if (broker == null) {
            return 0;
        }

        logger.info("[EventConsumerScanner] Scanning package '{}' for @EventConsumer annotations...", packageToScan);

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackages(packageToScan)
                .filterInputsBy(new FilterBuilder().includePackage(packageToScan))
                .setScanners(Scanners.TypesAnnotated));

        Set<Class<?>> consumerClasses = reflections.getTypesAnnotatedWith(EventConsumer.class);

        int registeredCount = 0;
        for (Class<?> clazz : consumerClasses) {

            if (!EventHandler.class.isAssignableFrom(clazz)) {
                logger.warn("[EventConsumerScanner] Class '{}' has @EventConsumer but does not implement EventHandler. Skipping.",
                        clazz.getName());
                continue;
            }

            EventConsumer annotation = clazz.getAnnotation(EventConsumer.class);
            if (annotation == null) continue;

            String destination = annotation.destination();
            if (destination == null || destination.isBlank()) {
                logger.warn("[EventConsumerScanner] Class '{}' specifies an empty destination topic. Skipping.", clazz.getName());
                continue;
            }

            try {
                @SuppressWarnings("unchecked")
                EventHandler<Object> handler = (EventHandler<Object>) clazz.getDeclaredConstructor().newInstance();

                @SuppressWarnings("unchecked")
                Class<Object> payloadClass = (Class<Object>) annotation.payloadType();

                ConsumerConfig config = ConsumerConfig.builder()
                        .concurrency(annotation.concurrency())
                        .maxRetries(annotation.maxRetries())
                        .backoffMillis(annotation.backoffMillis())
                        .backoffMultiplier(annotation.backoffMultiplier())
                        .build();

                // Since the broker is already active, subscribing here immediately starts
                // the worker thread pool to begin polling and processing messages for this topic.
                broker.subscribe(IConsumer.of(destination), config, payloadClass, handler);
                registeredCount++;

                logger.info("[EventConsumerScanner] Auto-registered @EventConsumer '{}' -> destination='{}' (payload={})",
                        clazz.getSimpleName(), destination, payloadClass.getSimpleName());

            } catch (ReflectiveOperationException e) {
                logger.error("[EventConsumerScanner] Failed to instantiate @EventConsumer class '{}'. Ensure it has a public no-arg constructor.",
                        clazz.getName(), e);
                throw new IllegalStateException("Failed to instantiate @EventConsumer class " + clazz.getName(), e);
            }
        }

        logger.info("[EventConsumerScanner] Discovered and registered {} @EventConsumer subscriber(s).", registeredCount);
        return registeredCount;
    }
}
