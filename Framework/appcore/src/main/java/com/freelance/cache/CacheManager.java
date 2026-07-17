package com.freelance.cache;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheDefinition;
import com.freelance.config.scanner.CacheConfigScanner;
import com.freelance.provider.Cache;
import com.freelance.provider.dbCache.DbCache;
import com.freelance.provider.memory.InMemoryCache;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central, thread-safe entry point for all caching in the application.
 * Discovers every AbstractCacheConfig subclass at boot and exposes a
 * simple get/put/invalidate/clear API to developers, who never see or
 * touch a cache's storage type, TTL, or loading logic — that all lives
 * in the cache's own AbstractCacheConfig subclass.
 */
public final class CacheManager {

    private static final String CLASS_NAME = CacheManager.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

    private static final class Holder {
        private static final CacheManager INSTANCE = new CacheManager();
    }

    public static CacheManager getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, AbstractCacheConfig<?, ?>> configsByName = new ConcurrentHashMap<>();
    private final Map<String, Cache> caches = new ConcurrentHashMap<>();
    private volatile EntityManagerFactory emf;
    private volatile boolean initialized = false;

    private CacheManager() {
    }

    /**
     * Discovers all AbstractCacheConfig subclasses under basePackage,
     * instantiates each (requires a public no-arg constructor), and
     * prepares the manager to serve them. Must be called once at
     * application boot, before any cache is used.
     */
    public synchronized void init(String basePackage, EntityManagerFactory emf) {
        if (initialized) return;
        this.emf = emf;

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

    /**
     * Retrieves a value from the named cache, invoking that cache's own
     * load(key) implementation on a miss or expiry.
     */
    @SuppressWarnings("unchecked")
    public <K extends Serializable, V extends Serializable> V get(String cacheName, K key) {
        AbstractCacheConfig<?, ?> config = resolveConfig(cacheName);
        Cache cache = resolveCache(cacheName, config);
        return (V) cache.get(String.valueOf(key), () -> config.loadInternal(key));
    }

    /**
     * Explicitly overwrites the cached value for key, bypassing load().
     */
    public <K extends Serializable, V extends Serializable> void put(String cacheName, K key, V value) {
        resolveCache(cacheName, resolveConfig(cacheName)).put(String.valueOf(key), value);
    }

    /**
     * Removes a single entry, forcing the next get() for that key to recompute.
     */
    public <K extends Serializable> void invalidate(String cacheName, K key) {
        Cache cache = caches.get(cacheName);
        if (cache != null) cache.invalidate(String.valueOf(key));
    }

    /**
     * Removes every entry in the named cache.
     */
    public void clear(String cacheName) {
        Cache cache = caches.get(cacheName);
        if (cache != null) cache.clear();
    }

    private AbstractCacheConfig<?, ?> resolveConfig(String cacheName) {
        if (!initialized) {
            throw new IllegalStateException("com.freelance.cache.CacheManager has not been initialized. Call init() at application boot.");
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
                case DB -> new DbCache(definition, emf);
            };
        });
    }
}