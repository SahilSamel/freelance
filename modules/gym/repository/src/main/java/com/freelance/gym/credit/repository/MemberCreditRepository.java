package com.freelance.gym.credit.repository;

import com.freelance.gym.credit.entity.MemberCreditEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MemberCreditRepository extends AbstractRepository<MemberCreditEntity, UUID> {

    public Optional<MemberCreditEntity> findByUserId(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return executeSingleResultSearch(MemberCreditEntity.QUERY_FIND_BY_USER_ID, Map.of("userId", userId));
    }

    public List<MemberCreditEntity> findByUserIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(MemberCreditEntity.QUERY_FIND_BY_USER_IDS, Map.of("userIds", userIds));
    }

    public int deleteByUserId(UUID userId) {
        if (userId == null) {
            return 0;
        }
        return executeUpdateQuery(MemberCreditEntity.QUERY_DELETE_BY_USER_ID, Map.of("userId", userId));
    }
}
