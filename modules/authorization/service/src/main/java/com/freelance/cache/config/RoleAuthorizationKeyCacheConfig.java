package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;

public class RoleAuthorizationKeyCacheConfig extends AbstractCacheConfig<String, RoleAuthorizationKeyCacheEntry> {
    public static final String NAME = "cache_RoleAuthorizationKey";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected RoleAuthorizationKeyCacheEntry load(String roleId) {
        return null;
    }
}
