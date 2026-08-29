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
                            Set<String> authKeys = entry.getValue().stream()
                                    .map(ra -> ra.getAuthorization() != null ? ra.getAuthorization().getKey() : null)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());
                            return new RoleAuthorizationKeyCacheEntry(authKeys);
                        }
                ));
    }

    public RoleAuthorizationKeyCacheEntry toRoleAuthorizationCacheEntry(List<RoleAuthorization> roleAuthorizations) {
        if (roleAuthorizations == null || roleAuthorizations.isEmpty()) {
            return new RoleAuthorizationKeyCacheEntry();
        }

        Set<String> authKeys = roleAuthorizations.stream()
                .map(ra -> ra.getAuthorization() != null ? ra.getAuthorization().getKey() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return new RoleAuthorizationKeyCacheEntry(authKeys);
    }
}