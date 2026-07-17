package com.freelance.config.data;

/**
 * Storage backend for a cache. Determines where com.freelance.cache.CacheManager persists
 * values for a given cache, as declared by that cache's AbstractCacheConfig.
 */
public enum CacheType {

    /**
     * Values live only in JVM memory (ConcurrentHashMap-backed). Lost on restart.
     */
    IN_MEMORY,

    /**
     * Values are persisted as a BLOB in the database, surviving restarts.
     */
    DB
}