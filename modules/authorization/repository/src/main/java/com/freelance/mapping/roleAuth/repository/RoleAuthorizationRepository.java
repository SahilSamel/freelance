package com.freelance.mapping.roleAuth.repository;

import com.freelance.mapping.roleAuth.entity.RoleAuthorization;
import com.freelance.mapping.roleAuth.entity.RoleAuthorizationKey;
import com.freelance.rest.app.repository.AbstractRepository;

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
}