package com.freelance.mapping.roleUser.repository;

import com.freelance.mapping.roleUser.entity.RoleUserEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.UUID;

public class RoleUserRepository extends AbstractRepository<RoleUserEntity, UUID> {

    public List<UUID> findRoleIdsByUserId(UUID userId) {
        return em().createQuery(
                "SELECT ur.roleId FROM UserRoleEntity ur WHERE ur.userId = :userId", UUID.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
