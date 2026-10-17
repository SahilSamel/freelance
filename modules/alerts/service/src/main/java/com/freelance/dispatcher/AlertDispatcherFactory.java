package com.freelance.dispatcher;

import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.dispatcher.impl.whatsapp.Msg91WhatsAppDispatcherService;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import com.freelance.keys.alerts.message.AlertEmitConfigKeys;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import com.freelance.system.SystemHardcoding;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory responsible for discovering and resolving {@link IAlertDispatcher} implementations.
 * Automatically scans the classpath for all implementations of {@link IAlertDispatcher}
 * upon first use, removing dispatcher-initialization coupling from callers.
 */
public class AlertDispatcherFactory {

    private static final Logger logger = LoggerFactory.getLogger(AlertDispatcherFactory.class);
    private static final String DEFAULT_SCAN_PACKAGE = "com.freelance";

    private final Map<AlertChannel, IAlertDispatcher> dispatchers = new ConcurrentHashMap<>();
    private final boolean autoDiscover;
    private volatile boolean discovered = false;

    public AlertDispatcherFactory() {
        this(true);
    }

    public AlertDispatcherFactory(boolean autoDiscover) {
        this.autoDiscover = autoDiscover;
    }

    public AlertDispatcherFactory(IAlertDispatcher... customDispatchers) {
        this(true);
        ensureDiscovered();
        if (customDispatchers != null) {
            for (IAlertDispatcher d : customDispatchers) {
                if (d != null) {
                    register(d);
                }
            }
        }
    }

    public AlertDispatcherFactory(Collection<IAlertDispatcher> customDispatchers) {
        this(true);
        ensureDiscovered();
        if (customDispatchers != null) {
            for (IAlertDispatcher d : customDispatchers) {
                if (d != null) {
                    register(d);
                }
            }
        }
    }

    private void ensureDiscovered() {
        if (!autoDiscover || discovered) {
            return;
        }
        synchronized (this) {
            if (!discovered) {
                discoverDispatchers();
                discovered = true;
            }
        }
    }

    private void discoverDispatchers() {
        logger.info("[AlertDispatcherFactory] Auto-discovering IAlertDispatcher implementations in package '{}'...", DEFAULT_SCAN_PACKAGE);
        try {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages(DEFAULT_SCAN_PACKAGE)
                    .filterInputsBy(new FilterBuilder().includePackage(DEFAULT_SCAN_PACKAGE))
                    .setScanners(Scanners.SubTypes));

            Set<Class<? extends IAlertDispatcher>> classes = reflections.getSubTypesOf(IAlertDispatcher.class);
            for (Class<? extends IAlertDispatcher> clazz : classes) {
                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                try {
                    Constructor<? extends IAlertDispatcher> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    IAlertDispatcher instance = constructor.newInstance();
                    register(instance);
                    logger.info("[AlertDispatcherFactory] Auto-registered dispatcher: {} for channel {}",
                            clazz.getSimpleName(), instance.getChannel());
                } catch (NoSuchMethodException e) {
                    logger.debug("[AlertDispatcherFactory] Class '{}' has no no-arg constructor, skipping auto-registration.", clazz.getName());
                } catch (Exception e) {
                    logger.warn("[AlertDispatcherFactory] Could not instantiate dispatcher '{}': {}", clazz.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("[AlertDispatcherFactory] Reflection scanning failed: {}", e.getMessage());
        }

        // Ensure default built-in dispatchers are present if not found by reflection
        if (!dispatchers.containsKey(AlertChannel.WHATSAPP)) {
            String provider = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_PROVIDER);
            if (SystemHardcoding.META.asString().equalsIgnoreCase(provider)) {
                register(new WhatsAppDispatcherService());
            } else {
                register(new Msg91WhatsAppDispatcherService());
            }
        }
    }

    public void register(IAlertDispatcher dispatcher) {
        if (dispatcher != null && dispatcher.getChannel() != null) {
            dispatchers.put(dispatcher.getChannel(), dispatcher);
            logger.info("[AlertDispatcherFactory] Registered dispatcher: {} for channel: {}",
                    dispatcher.getClass().getSimpleName(), dispatcher.getChannel());
        }
    }

    public void unregister(AlertChannel channel) {
        ensureDiscovered();
        if (channel != null) {
            dispatchers.remove(channel);
            logger.info("[AlertDispatcherFactory] Unregistered dispatcher for channel: {}", channel);
        }
    }

    public boolean hasDispatcher(AlertChannel channel) {
        ensureDiscovered();
        return channel != null && dispatchers.containsKey(channel);
    }

    public Optional<IAlertDispatcher> getDispatcher(AlertChannel requestedChannel) {
        ensureDiscovered();

        if (requestedChannel == AlertChannel.WHATSAPP && dispatchers.containsKey(AlertChannel.WHATSAPP)) {
            String provider = ConfigurationEngine.get(WebhookConfigKeys.WHATSAPP_PROVIDER);
            IAlertDispatcher current = dispatchers.get(AlertChannel.WHATSAPP);
            if (SystemHardcoding.META.asString().equalsIgnoreCase(provider)) {
                if (current instanceof WhatsAppDispatcherService) {
                    return Optional.of(current);
                }
                return Optional.of(new WhatsAppDispatcherService());
            } else {
                if (current instanceof Msg91WhatsAppDispatcherService) {
                    return Optional.of(current);
                }
                return Optional.of(new Msg91WhatsAppDispatcherService());
            }
        }

        if (requestedChannel != null && dispatchers.containsKey(requestedChannel)) {
            return Optional.of(dispatchers.get(requestedChannel));
        }

        // Fallback to configured default channel
        AlertChannel defaultChannel = ConfigurationEngine.getEnum(
                AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL,
                AlertChannel.class,
                AlertChannel.WHATSAPP
        );

        if (defaultChannel != null && defaultChannel != requestedChannel && dispatchers.containsKey(defaultChannel)) {
            logger.warn(
                    "[AlertDispatcherFactory] Channel {} not available, falling back to default channel: {}",
                    requestedChannel,
                    defaultChannel
            );
            return getDispatcher(defaultChannel);
        }

        logger.error(
                "[AlertDispatcherFactory] No dispatcher available for channel {} (fallback to {} also unavailable)",
                requestedChannel,
                defaultChannel
        );
        return Optional.empty();
    }

    public Map<AlertChannel, IAlertDispatcher> getRegisteredDispatchers() {
        ensureDiscovered();
        return Collections.unmodifiableMap(dispatchers);
    }
}
