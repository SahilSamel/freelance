package com.freelance.gym.userplan.repository;

import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserPlanRepository extends AbstractRepository<UserPlanEntity, UUID> {

    public List<UserPlanEntity> findByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return executeListResultSearch(UserPlanEntity.QUERY_FIND_BY_USER_ID, Map.of("userId", userId));
    }

    public Optional<UserPlanEntity> findLatestByUserAndPlan(UUID userId, UUID planId) {
        if (userId == null || planId == null) {
            return Optional.empty();
        }
        List<UserPlanEntity> list = executeListResultSearch(UserPlanEntity.QUERY_FIND_BY_USER_AND_PLAN, Map.of("userId", userId, "planId", planId));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<UserPlanEntity> findByUserIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(UserPlanEntity.QUERY_FIND_BY_USER_IDS, Map.of("userIds", userIds));
    }

    public int deleteByUserId(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return executeUpdateQuery(UserPlanEntity.QUERY_DELETE_BY_USER_ID, Map.of("userId", userId));
    }
}
