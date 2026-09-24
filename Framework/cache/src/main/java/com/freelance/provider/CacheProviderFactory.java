package com.freelance.provider;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheDefinition;
import com.freelance.provider.impl.InMemoryCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle and instantiation of underlying {@link Cache} provider instances.
 */
public class CacheProviderFactory {

    private static final class Holder {
        private static final CacheProviderFactory INSTANCE = new CacheProviderFactory();
    }

    public static CacheProviderFactory getInstance() {
        return Holder.INSTANCE;
    }

    private final Map<String, Cache> caches = new ConcurrentHashMap<>();

    public Cache getOrCreate(String cacheName, AbstractCacheConfig<?, ?> config) {
        return caches.computeIfAbsent(cacheName, name -> {
            CacheDefinition definition = config.toDefinition();
            return switch (definition.type()) {
                case IN_MEMORY -> new InMemoryCache(definition);
            };
        });
    }

    public Cache get(String cacheName) {
        return caches.get(cacheName);
    }

    public void reset() {
        caches.clear();
    }
}
