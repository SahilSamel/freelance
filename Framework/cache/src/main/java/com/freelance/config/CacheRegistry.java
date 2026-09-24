package com.freelance.config;

import com.freelance.config.scanner.CacheConfigScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry responsible for discovering, validating, and holding
 * {@link AbstractCacheConfig} configurations by name and class token.
 */
public class CacheRegistry {

    public static final String MANDATED_CACHE_CONFIG_PACKAGE = "com.freelance.cache.config";

    private static final Logger logger = LoggerFactory.getLogger(CacheRegistry.class);

    private static final class Holder {
        private static final CacheRegistry INSTANCE = new CacheRegistry();
    }

    public static CacheRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, AbstractCacheConfig<?, ?>> configsByName = new ConcurrentHashMap<>();
    private final Map<Class<? extends AbstractCacheConfig<?, ?>>, String> nameByClass = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public synchronized void init() {
        init(MANDATED_CACHE_CONFIG_PACKAGE);
    }

    /**
     * Scans the specified base package for {@link AbstractCacheConfig} implementations,
     * instantiates them, validates uniqueness, and registers them.
     *
     * @param basePackage the root package to scan
     * @throws IllegalStateException if duplicate cache names or instantiation errors occur
     */
    public synchronized void init(String basePackage) {
        if (initialized) return;

        CacheConfigScanner scanner = new CacheConfigScanner();
        Set<Class<? extends AbstractCacheConfig<?, ?>>> configClasses = scanner.scan(basePackage);

        for (Class<? extends AbstractCacheConfig<?, ?>> configClass : configClasses) {
            AbstractCacheConfig<?, ?> instance = instantiate(configClass);
            String name = instance.toDefinition().name();

            AbstractCacheConfig<?, ?> existing = configsByName.putIfAbsent(name, instance);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate cache name '" + name + "' defined by both " +
                                existing.getClass().getName() + " and " + configClass.getName());
            }

            nameByClass.put(configClass, name);

            logger.debug("[CacheRegistry] Registered cache '{}' -> {} (type={}, ttl={})",
                    name, configClass.getSimpleName(),
                    instance.toDefinition().type(), instance.toDefinition().ttlSeconds());
        }

        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public synchronized void reset() {
        configsByName.clear();
        nameByClass.clear();
        initialized = false;
    }

    public AbstractCacheConfig<?, ?> resolveConfig(String cacheName) {
        ensureInitialized();
        AbstractCacheConfig<?, ?> config = configsByName.get(cacheName);
        if (config == null) {
            throw new IllegalStateException(
                    "No cache named '" + cacheName + "' is registered. " +
                            "Create an AbstractCacheConfig subclass whose name() returns '" + cacheName + "'.");
        }
        return config;
    }

    public String resolveName(Class<?> configClass) {
        ensureInitialized();
        if (configClass == null) {
            return null;
        }
        String name = nameByClass.get(configClass);
        if (name == null) {
            throw new IllegalStateException(
                    "No cache registered for class '" + configClass.getName() + "'.");
        }
        return name;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("com.freelance.config.CacheRegistry has not been initialized. Call init() at application boot.");
        }
    }

    private AbstractCacheConfig<?, ?> instantiate(Class<? extends AbstractCacheConfig<?, ?>> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to instantiate cache config '" + configClass.getName() +
                            "'. Ensure it has a public no-arg constructor.", e);
        }
    }
}
