package com.freelance.cache.config;

import com.freelance.ConfigurationEngine;
import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheType;

/**
 * Backs ConfigurationEngine's error message lookups. The actual DB read
 * lives in ConfigurationEngine; this class only wires that lookup into
 * the cache.
 */
public class ErrorMessageCache extends AbstractCacheConfig<String, String> {

    public static final String NAME = "cache_ErrorMessage";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected CacheType type() {
        return CacheType.IN_MEMORY;
    }

    @Override
    protected String load(String errorId) {
        return ConfigurationEngine.loadErrorMessage(errorId);
    }
}