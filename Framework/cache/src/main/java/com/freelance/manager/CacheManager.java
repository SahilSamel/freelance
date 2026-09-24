package com.freelance.manager;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.CacheRegistry;
import com.freelance.provider.Cache;
import com.freelance.provider.CacheProviderFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * Static facade for application caching operations.
 * <p>
 * Delegates configuration resolution to {@link CacheRegistry}, tenant key isolation to
 * {@link TenantCacheKeyResolver}, and cache instances to {@link CacheProviderFactory}.
 * </p>
 */
public final class CacheManager {

    private CacheManager() {
    }

    @SuppressWarnings("unchecked")
    public static <K extends Serializable, V extends Serializable> V get(String cacheName, K key) {
        if (cacheName == null || key == null) {
            return null;
        }

        AbstractCacheConfig<?, ?> config = CacheRegistry.getInstance().resolveConfig(cacheName);
        Cache cache = CacheProviderFactory.getInstance().getOrCreate(cacheName, config);
        String tenantKey = TenantCacheKeyResolver.getInstance().resolveTenantKey(key);
        return (V) cache.get(tenantKey, () -> config.loadInternal(key));
    }

    public static <K extends Serializable, V extends Serializable, C extends AbstractCacheConfig<K, V>> V get(Class<C> configClass, K key) {
        return get(CacheRegistry.getInstance().resolveName(configClass), key);
    }

    public static <K extends Serializable, V extends Serializable> void put(String cacheName, K key, V value) {
        if (cacheName == null || key == null || value == null) {
            return;
        }

        AbstractCacheConfig<?, ?> config = CacheRegistry.getInstance().resolveConfig(cacheName);
        Cache cache = CacheProviderFactory.getInstance().getOrCreate(cacheName, config);
        cache.put(TenantCacheKeyResolver.getInstance().resolveTenantKey(key), value);
    }

    public static <K extends Serializable, V extends Serializable, C extends AbstractCacheConfig<K, V>> void put(Class<C> configClass, K key, V value) {
        put(CacheRegistry.getInstance().resolveName(configClass), key, value);
    }

    public static <K extends Serializable, V extends Serializable> void preload(String cacheName, Map<K, V> entries) {
        if (cacheName == null || entries == null || entries.isEmpty()) {
            return;
        }

        AbstractCacheConfig<?, ?> config = CacheRegistry.getInstance().resolveConfig(cacheName);
        Cache cache = CacheProviderFactory.getInstance().getOrCreate(cacheName, config);

        entries.forEach((key, value) -> {
            if (key != null && value != null) {
                cache.put(TenantCacheKeyResolver.getInstance().resolveTenantKey(key), value);
            }
        });
    }

    public static <K extends Serializable, V extends Serializable, C extends AbstractCacheConfig<K, V>> void preload(Class<C> configClass, Map<K, V> entries) {
        preload(CacheRegistry.getInstance().resolveName(configClass), entries);
    }

    public static <K extends Serializable> void invalidate(String cacheName, K key) {
        if (cacheName == null || key == null) {
            return;
        }

        Cache cache = CacheProviderFactory.getInstance().get(cacheName);
        if (cache != null) {
            cache.invalidate(TenantCacheKeyResolver.getInstance().resolveTenantKey(key));
        }
    }

    public static <K extends Serializable, C extends AbstractCacheConfig<K, ?>> void invalidate(Class<C> configClass, K key) {
        invalidate(CacheRegistry.getInstance().resolveName(configClass), key);
    }

    public static void clear(String cacheName) {
        if (cacheName == null) {
            return;
        }

        Cache cache = CacheProviderFactory.getInstance().get(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public static <C extends AbstractCacheConfig<?, ?>> void clear(Class<C> configClass) {
        clear(CacheRegistry.getInstance().resolveName(configClass));
    }
}