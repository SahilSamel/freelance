package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheType;

import java.util.UUID;

public class SessionCacheConfig extends AbstractCacheConfig<UUID, SessionCacheEntry> {

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
    protected SessionCacheEntry load(UUID userId) {
        return null;
    }

    @Override
    public long ttlSeconds() {
        return 600;
    }
}
