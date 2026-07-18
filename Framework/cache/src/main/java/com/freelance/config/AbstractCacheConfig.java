package com.freelance.config;

import com.freelance.config.data.CacheDefinition;
import com.freelance.config.data.CacheType;

import java.io.Serializable;

/**
 * Base class for defining a cache. Each concrete subclass represents
 * exactly one named cache: its storage type, TTL, and how to load a
 * value on a miss, all in one place.
 *
 * @param <K> the cache key type
 * @param <V> the cached value type
 */
public abstract class AbstractCacheConfig<K extends Serializable, V extends Serializable> {

    /**
     * Cache name used by developers to call com.freelance.manager.CacheManager.get(name, key). Defaults to the class name.
     */
    protected String name() {
        return getClass().getSimpleName();
    }

    /**
     * Time-to-live in seconds. Defaults to infinite (-1).
     */
    protected long ttlSeconds() {
        return -1;
    }

    /**
     * Storage backend for this cache. Defaults to in-memory.
     */
    protected CacheType type() {
        return CacheType.IN_MEMORY;
    }

    /**
     * Computes the value for a given key on a cache miss or expiry.
     * This is the only method every designer MUST implement.
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