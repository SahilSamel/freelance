package com.freelance.mapping.roleAuthorization.repository;

import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorizationKey;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.*;
import java.util.stream.Collectors;

public class RoleAuthorizationRepository extends AbstractRepository<RoleAuthorization, RoleAuthorizationKey> {

    public Map<UUID, List<RoleAuthorization>> getAllMappingsGroupedByRoleId() {
        return executeListResultSearch(RoleAuthorization.QUERY_FIND_ALL_WITH_DETAILS, null)
                .stream()
                .collect(Collectors.groupingBy(ra -> ra.getId().getRoleId()));
    }

    public List<RoleAuthorization> getRoleAuthorizationsByRoleId(UUID roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return executeListResultSearch(RoleAuthorization.QUERY_FIND_BY_ROLE_ID, Map.of("roleId", roleId));
    }

    public int deleteBySystemRoles() {
        return executeUpdateQuery(RoleAuthorization.QUERY_DELETE_BY_SYSTEM_ROLES, null);
    }

    public Map<RoleAuthorizationKey, RoleAuthorization> findAllSystemRoleMappings() {
        List<RoleAuthorization> list = executeListResultSearch(RoleAuthorization.QUERY_FIND_ALL_SYSTEM_ROLE_MAPPINGS, null);
        Map<RoleAuthorizationKey, RoleAuthorization> map = new HashMap<>();
        for (RoleAuthorization ra : list) {
            map.put(ra.getId(), ra);
        }
        return map;
    }

    public void removeByRoleId(UUID roleId) {
        if (roleId == null) return;
        executeUpdateQuery(RoleAuthorization.QUERY_DELETE_BY_ROLE_ID, Map.of("roleId", roleId));
    }
}