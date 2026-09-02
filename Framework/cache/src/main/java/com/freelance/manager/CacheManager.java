package com.freelance.manager;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheDefinition;
import com.freelance.config.scanner.CacheConfigScanner;
import com.freelance.provider.Cache;
import com.freelance.provider.impl.InMemoryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe singleton registry and management coordinator for all application caches.
 */
public final class CacheManager {

    private static final String CLASS_NAME = CacheManager.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    public static final String MANDATED_CACHE_CONFIG_PACKAGE = "com.freelance.cache.config";

    private static final class Holder {
        private static final CacheManager INSTANCE = new CacheManager();
    }

    public static CacheManager getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, AbstractCacheConfig<?, ?>> configsByName = new ConcurrentHashMap<>();
    private final Map<Class<? extends AbstractCacheConfig<?, ?>>, String> nameByClass = new ConcurrentHashMap<>();
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    private CacheManager() {
    }

    /**
     * Discovers and registers all {@link AbstractCacheConfig} implementations in the mandated
     * package {@value #MANDATED_CACHE_CONFIG_PACKAGE}.
     * <p>
     * Must be invoked during application boot before accessing any cache operations.
     * </p>
     *
     * @throws IllegalStateException if duplicate cache names are detected or configuration instantiation fails
     */
    public synchronized void init() {
        init(MANDATED_CACHE_CONFIG_PACKAGE);
    }

    /**
     * Discovers and registers all {@link AbstractCacheConfig} implementations in the specified base package.
     * <p>
     * Must be invoked during application boot before accessing any cache operations.
     * </p>
     *
     * @param basePackage the root package name to scan
     * @throws IllegalStateException if duplicate cache names are detected or configuration instantiation fails
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

            logger.debug("[{}] Registered cache '{}' -> {} (type={}, ttl={})",
                    CLASS_NAME, name, configClass.getSimpleName(),
                    instance.toDefinition().type(), instance.toDefinition().ttlSeconds());
        }

        this.initialized = true;
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

    @SuppressWarnings("unchecked")
    public <K extends Serializable, V extends Serializable> V get(String cacheName, K key) {
        if (cacheName == null || key == null) {
            return null;
        }

        AbstractCacheConfig<?, ?> config = resolveConfig(cacheName);
        Cache cache = resolveCache(cacheName, config);
        return (V) cache.get(String.valueOf(key), () -> config.loadInternal(key));
    }

    public <K extends Serializable, V extends Serializable, C extends AbstractCacheConfig<K, V>> V get(Class<C> configClass, K key) {
        String cacheName = resolveName(configClass);
        return get(cacheName, key);
    }

    public <K extends Serializable, V extends Serializable> void put(String cacheName, K key, V value) {
        if (cacheName == null || key == null || value == null) {
            return;
        }

        resolveCache(cacheName, resolveConfig(cacheName)).put(String.valueOf(key), value);
    }

    public <K extends Serializable, V extends Serializable, C extends AbstractCacheConfig<K, V>> void put(Class<C> configClass, K key, V value) {
        String cacheName = resolveName(configClass);
        put(cacheName, key, value);
    }

    /**
     * Preloads multiple key-value pairs into the specified cache in a single operation.
     *
     * @param <K>       the key type
     * @param <V>       the value type
     * @param cacheName the registered cache identifier
     * @param entries   map containing entries to populate; ignored if null or empty
     */
    public <K extends Serializable, V extends Serializable> void preload(String cacheName, Map<K, V> entries) {
        if (cacheName == null || entries == null || entries.isEmpty()) {
            return;
        }

        AbstractCacheConfig<?, ?> config = resolveConfig(cacheName);
        Cache cache = resolveCache(cacheName, config);

        entries.forEach((key, value) -> {
            if (key != null && value != null) {
                cache.put(String.valueOf(key), value);
            }
        });
    }

    /**
     * Type-safe overload: preloads multiple entries using the configuration class token.
     *
     * @param <K>         the key type
     * @param <V>         the value type
     * @param <C>         the configuration class type
     * @param configClass the cache configuration class
     * @param entries     map containing entries to populate
     */
    public <K extends Serializable, V extends Serializable, C extends AbstractCacheConfig<K, V>> void preload(Class<C> configClass, Map<K, V> entries) {
        String cacheName = resolveName(configClass);
        preload(cacheName, entries);
    }

    public <K extends Serializable> void invalidate(String cacheName, K key) {
        if (cacheName == null || key == null) {
            return;
        }

        Cache cache = caches.get(cacheName);
        if (cache != null) cache.invalidate(String.valueOf(key));
    }

    public <K extends Serializable, C extends AbstractCacheConfig<K, ?>> void invalidate(Class<C> configClass, K key) {
        String cacheName = resolveName(configClass);
        invalidate(cacheName, key);
    }

    public void clear(String cacheName) {
        if (cacheName == null) {
            return;
        }

        Cache cache = caches.get(cacheName);
        if (cache != null) cache.clear();
    }

    public <C extends AbstractCacheConfig<?, ?>> void clear(Class<C> configClass) {
        String cacheName = resolveName(configClass);
        clear(cacheName);
    }

    private String resolveName(Class<?> configClass) {
        if (!initialized) {
            throw new IllegalStateException("com.freelance.manager.CacheManager has not been initialized. Call init() at application boot.");
        }
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

    private AbstractCacheConfig<?, ?> resolveConfig(String cacheName) {
        if (!initialized) {
            throw new IllegalStateException("com.freelance.manager.CacheManager has not been initialized. Call init() at application boot.");
        }
        AbstractCacheConfig<?, ?> config = configsByName.get(cacheName);
        if (config == null) {
            throw new IllegalStateException(
                    "No cache named '" + cacheName + "' is registered. " +
                            "Create an AbstractCacheConfig subclass whose name() returns '" + cacheName + "'.");
        }
        return config;
    }

    private Cache resolveCache(String cacheName, AbstractCacheConfig<?, ?> config) {
        return caches.computeIfAbsent(cacheName, name -> {
            CacheDefinition definition = config.toDefinition();
            return switch (definition.type()) {
                case IN_MEMORY -> new InMemoryCache(definition);
            };
        });
    }
}