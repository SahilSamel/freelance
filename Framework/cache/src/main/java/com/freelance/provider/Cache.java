package com.freelance.provider;

import java.util.function.Supplier;

/**
 * A single named cache's storage contract. Implementations decide where
 * values physically live (memory, DB, ...); Cache Manager decides
 * which implementation backs a given cache name based on its Cache Manager.
 * <p>
 * Public because CacheManager (in the appcore module) depends on this type
 * across the module boundary; not intended to be implemented outside this package.
 */
public interface Cache {

    /**
     * Returns the cached value for key if present and not expired.
     * On a miss or expiry, invokes loader to compute the value, stores
     * it under key according to this cache's configured TTL, and
     * returns it.
     *
     * @param key    the cache key, already converted to its string form
     * @param loader supplies the value to store and return on a miss;
     *               only invoked if no valid cached value exists
     * @return the cached or freshly loaded value
     */
    Object get(String key, Supplier<Object> loader);

    /**
     * Stores value under key, overwriting any existing entry and
     * resetting its expiry according to this cache's configured TTL.
     *
     * @param key   the cache key, already converted to its string form
     * @param value Object to be cached
     */
    void put(String key, Object value);

    /**
     * Removes the entry for key, if present. A subsequent get() for
     * this key will be treated as a miss.
     */
    void invalidate(String key);

    /**
     * Removes every entry from this cache.
     */
    void clear();
}