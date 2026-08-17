package com.freelance.mapping.roleAuthorization.assembler;

import com.freelance.cache.config.RoleAuthorizationKeyCacheEntry;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorization;

import java.util.*;
import java.util.stream.Collectors;

public class RoleAuthorizationAssembler {

    public Map<UUID, RoleAuthorizationKeyCacheEntry> toRoleIdToCacheEntryMap(Map<UUID, List<RoleAuthorization>> roleAuthMappings) {
        if (roleAuthMappings == null || roleAuthMappings.isEmpty()) {
            return Collections.emptyMap();
        }

        return roleAuthMappings.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Set<UUID> authIds = entry.getValue().stream()
                                    .map(ra -> ra.getId().getAuthorizationId())
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());
                            return new RoleAuthorizationKeyCacheEntry(authIds);
                        }
                ));
    }

    public RoleAuthorizationKeyCacheEntry toRoleAuthorizationCacheEntry(List<RoleAuthorization> roleAuthorizations) {
        if (roleAuthorizations == null || roleAuthorizations.isEmpty()) {
            return new RoleAuthorizationKeyCacheEntry();
        }

        Set<UUID> authIds = roleAuthorizations.stream()
                .map(ra -> ra.getId().getAuthorizationId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return new RoleAuthorizationKeyCacheEntry(authIds);
    }
}