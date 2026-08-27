package com.freelance.provider;

import java.util.function.Supplier;

/**
 * Defines the contract for physical cache storage backends.
 * <p>
 * Implementations manage entry storage, TTL expiration, and programmatic eviction.
 * </p>
 */
public interface Cache {

    /**
     * Retrieves the cached value for the specified key, or computes, stores, and returns a fresh value using the loader.
     * <p>
     * Implementations are expected to return the cached value if present and unexpired. On a cache miss or expiration,
     * implementations are expected to invoke the provided {@code loader} supplier (if non-null), cache the returned
     * non-null value, and return it. If the key is absent and no loader is supplied, implementations must return {@code null}.
     * </p>
     *
     * @param key    string identifier of the cache entry; must not be null
     * @param loader fallback supplier invoked to produce and cache the value on a cache miss; may be null
     * @return the cached or freshly loaded value, or {@code null} if absent and unproduced
     */
    Object get(String key, Supplier<Object> loader);

    /**
     * Stores a key-value mapping in the cache, overwriting any existing entry and resetting its expiration time.
     * <p>
     * Implementations are expected to store the value under the specified key and apply the configured TTL.
     * If either the key or value is null, implementations are expected to ignore the operation as a safe no-op.
     * </p>
     *
     * @param key   string identifier of the cache entry; must not be null
     * @param value object payload to store; must not be null
     */
    void put(String key, Object value);

    /**
     * Evicts a single entry associated with the specified key from the cache.
     * <p>
     * Implementations are expected to remove the entry so that subsequent reads result in a cache miss.
     * If the key is null or does not exist, implementations are expected to safely ignore the call.
     * </p>
     *
     * @param key string identifier of the cache entry to remove
     */
    void invalidate(String key);

    /**
     * Removes all entries from the cache.
     * <p>
     * Implementations are expected to purge all cached keys and values, resetting the cache to an empty state.
     * </p>
     */
    void clear();
}