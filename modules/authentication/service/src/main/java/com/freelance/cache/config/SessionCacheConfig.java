package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheType;

public class SessionCacheConfig extends AbstractCacheConfig<String, SessionCacheEntry> {

    public static final String NAME = "cache_Session";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected CacheType type() {
        return CacheType.IN_MEMORY;
    }

    @Override
    protected SessionCacheEntry load(String sessionKey) {
        return null;
    }

    @Override
    public long ttlSeconds() {
        return 600;
    }
}
