package com.freelance.config.data;

/**
 * Immutable description of a single cache: its name, time-to-live, and
 * storage backend. Built internally from an AbstractCacheConfig subclass
 * via toDefinition() — not constructed directly by designers or developers.
 *
 * @param name       cache name, used by developers to call com.freelance.cache.CacheManager.get(name, key)
 * @param ttlSeconds time-to-live in seconds; -1 means the entry never expires
 * @param type       where values for this cache are stored
 */
public record CacheDefinition(String name, long ttlSeconds, CacheType type) {

    public CacheDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cache name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Cache type cannot be null");
        }
    }

    /**
     * Whether entries in this cache expire, as opposed to living forever.
     */
    public boolean hasExpiry() {
        return ttlSeconds > 0;
    }
}