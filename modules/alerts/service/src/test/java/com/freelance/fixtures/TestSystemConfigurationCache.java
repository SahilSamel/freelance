package com.freelance.fixtures;

import com.freelance.config.AbstractCacheConfig;

public class TestSystemConfigurationCache extends AbstractCacheConfig<String, String> {

    @Override
    protected String name() {
        return "cache_SystemConfig";
    }

    @Override
    protected String load(String key) {
        return null;
    }
}