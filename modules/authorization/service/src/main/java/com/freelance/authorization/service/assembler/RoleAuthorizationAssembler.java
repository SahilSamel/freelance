package com.freelance.authorization.service.assembler;

import com.freelance.mapping.roleAuth.entity.RoleAuthorization;

import java.util.*;
import java.util.stream.Collectors;

public class RoleAuthorizationAssembler {

    public Map<UUID, Set<UUID>> toRoleIdToAuthIdsMap(Map<UUID, List<RoleAuthorization>> roleAuthMappings) {
        if (roleAuthMappings == null || roleAuthMappings.isEmpty()) {
            return Collections.emptyMap();
        }

        return roleAuthMappings.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(ra -> ra.getId().getAuthorizationId())
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                ));
    }
}