package com.freelance.repository.mapping.roleAuthorization;

import com.freelance.entity.mapping.roleAuthorization.RoleAuthorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorizationKey;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoleAuthorizationRepository extends AbstractRepository<RoleAuthorization, RoleAuthorizationKey> {

    public Map<UUID, List<RoleAuthorization>> getAllMappingsGroupedByRoleId() {
        String jpql =
            """
            SELECT ra 
            FROM RoleAuthorization ra 
            JOIN FETCH ra.authorization
            JOIN FETCH ra.role
            """;

        return em().createQuery(jpql, RoleAuthorization.class)
                .getResultList()
                .stream()
                .collect(Collectors.groupingBy(ra -> ra.getId().getRoleId()));
    }

    public List<RoleAuthorization> getRoleAuthorizationsByRoleId(UUID roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }

        String jpql =
                """
                SELECT ra 
                FROM RoleAuthorization ra 
                WHERE ra.id.roleId = :roleId
                """;

        return em().createQuery(jpql, RoleAuthorization.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }
}