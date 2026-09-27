package com.freelance.gym.usertype.repository;

import com.freelance.gym.usertype.entity.UserUserTypeEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserUserTypeRepository extends AbstractRepository<UserUserTypeEntity, UUID> {

    public List<UserUserTypeEntity> findByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return executeListResultSearch(UserUserTypeEntity.QUERY_FIND_BY_USER_ID, Map.of("userId", userId));
    }

    public List<UserUserTypeEntity> findAllByUserId(UUID userId) {
        return findByUserId(userId);
    }

    public List<UserUserTypeEntity> findTypesForUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(UserUserTypeEntity.QUERY_FIND_TYPES_FOR_USERS, Map.of("userIds", userIds));
    }

    public int deleteByUserId(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return executeUpdateQuery(UserUserTypeEntity.QUERY_DELETE_BY_USER_ID, Map.of("userId", userId));
    }

    public List<UUID> findUserIdsByUserTypeId(UUID userTypeId) {
        if (userTypeId == null) {
            return List.of();
        }
        return executeTypedListQuery(UserUserTypeEntity.QUERY_FIND_USER_IDS_BY_USER_TYPE_ID, UUID.class, Map.of("userTypeId", userTypeId));
    }
}

