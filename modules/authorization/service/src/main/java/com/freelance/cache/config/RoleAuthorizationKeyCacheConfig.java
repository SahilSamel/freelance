package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.mapping.roleAuthorization.RoleAuthorizationService;

import java.util.UUID;

public class RoleAuthorizationKeyCacheConfig extends AbstractCacheConfig<UUID, RoleAuthorizationKeyCacheEntry> {

    public static final String NAME = "cache_RoleAuthorizationKey";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected RoleAuthorizationKeyCacheEntry load(UUID roleId) {
        RoleAuthorizationService roleAuthorizationService = new RoleAuthorizationService();
        return roleAuthorizationService.loadRoleAuthorizationCacheEntryByRoleId(roleId);
    }
}
