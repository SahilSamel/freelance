package com.freelance.config;

import com.freelance.config.data.CacheDefinition;
import com.freelance.config.data.CacheType;

import java.io.Serializable;

/**
 * Serves as the abstract base class for defining typed application caches.
 * <p>
 * <b>Package Requirement:</b> All implementations extending this class <b>MUST</b> be located
 * within the package {@code com.freelance.cache.config} across any module.
 * {@code CacheManager} scans this specific package at application startup to discover, instantiate,
 * and register all cache definitions. Classes placed in any other package will not be discovered.
 * </p>
 *
 * @param <K> the cache key type
 * @param <V> the cached value type
 */
public abstract class AbstractCacheConfig<K extends Serializable, V extends Serializable> {

    protected String name() {
        return getClass().getSimpleName();
    }

    protected long ttlSeconds() {
        return -1;
    }

    protected CacheType type() {
        return CacheType.IN_MEMORY;
    }

    /**
     * Loads the cache entry value synchronously when no entry is present for the specified key.
     * <p>
     * Invoked synchronously during a cache miss to retrieve the value from the underlying data source
     * and populate the cache.
     * </p>
     *
     * @param key the requested cache key; guaranteed non-null
     * @return the loaded value to store in the cache, or {@code null} if no entry exists for the key
     */
    protected abstract V load(K key);

    public final CacheDefinition toDefinition() {
        return new CacheDefinition(name(), ttlSeconds(), type());
    }

    @SuppressWarnings("unchecked")
    public final V loadInternal(Object key) {
        return load((K) key);
    }
}