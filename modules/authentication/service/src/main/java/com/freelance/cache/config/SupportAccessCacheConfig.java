package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheType;

/**
 * In-memory cache configuration for managing active and pending vendor support access windows.
 */
public class SupportAccessCacheConfig extends AbstractCacheConfig<String, SupportAccessCacheEntry> {

    public static final String NAME = "cache_SupportAccess";
    public static final String KEY_ACTIVE_WINDOW = "ACTIVE_WINDOW";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected CacheType type() {
        return CacheType.IN_MEMORY;
    }

    @Override
    protected SupportAccessCacheEntry load(String key) {
        return new SupportAccessCacheEntry();
    }

    @Override
    public long ttlSeconds() {
        return -1;
    }
}
