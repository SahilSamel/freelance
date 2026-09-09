package com.freelance.mapping.userRole.repository;

import com.freelance.mapping.userRole.entity.UserRoleEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.UUID;

public class UserRoleRepository extends AbstractRepository<UserRoleEntity, UUID> {

    public List<UUID> findRoleIdsByUserId(UUID userId) {
        return em().createQuery(
                "SELECT ur.roleId FROM UserRoleEntity ur WHERE ur.userId = :userId", UUID.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
