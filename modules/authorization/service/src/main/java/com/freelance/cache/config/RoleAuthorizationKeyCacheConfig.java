package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.mapping.roleAuthorization.RoleAuthorizationService;

import java.util.UUID;

public class RoleAuthorizationKeyCacheConfig extends AbstractCacheConfig<String, RoleAuthorizationKeyCacheEntry> {

    public static final String NAME = "cache_RoleAuthorizationKey";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected RoleAuthorizationKeyCacheEntry load(String roleId) {
        RoleAuthorizationService roleAuthorizationService = new RoleAuthorizationService();
        return roleAuthorizationService.loadRoleAuthorizationCacheEntryByRoleId(UUID.fromString(roleId));
    }
}
