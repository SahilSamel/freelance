package com.freelance.fixtures;

import com.freelance.cache.config.SessionCacheEntry;
import com.freelance.config.AbstractCacheConfig;

import java.util.UUID;

public class TestSessionCache extends AbstractCacheConfig<UUID, SessionCacheEntry> {

    @Override
    protected String name() {
        return "cache_Session";
    }

    @Override
    protected SessionCacheEntry load(UUID key) {
        return null;
    }
}
