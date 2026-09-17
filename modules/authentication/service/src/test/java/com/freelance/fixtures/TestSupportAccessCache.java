package com.freelance.fixtures;

import com.freelance.cache.config.SupportAccessCacheEntry;
import com.freelance.config.AbstractCacheConfig;

public class TestSupportAccessCache extends AbstractCacheConfig<String, SupportAccessCacheEntry> {

    @Override
    protected String name() {
        return "cache_SupportAccess";
    }

    @Override
    protected SupportAccessCacheEntry load(String key) {
        return new SupportAccessCacheEntry();
    }
}
