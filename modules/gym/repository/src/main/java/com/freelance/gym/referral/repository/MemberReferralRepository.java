package com.freelance.gym.referral.repository;

import com.freelance.gym.referral.entity.MemberReferralEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MemberReferralRepository extends AbstractRepository<MemberReferralEntity, UUID> {

    public Optional<MemberReferralEntity> findByRefereeId(UUID refereeId) {
        if (refereeId == null) {
            return Optional.empty();
        }
        return executeSingleResultSearch(MemberReferralEntity.QUERY_FIND_BY_REFEREE_ID, Map.of("refereeId", refereeId));
    }

    public List<MemberReferralEntity> findByRefereeIds(List<UUID> refereeIds) {
        if (refereeIds == null || refereeIds.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(MemberReferralEntity.QUERY_FIND_BY_REFEREE_IDS, Map.of("refereeIds", refereeIds));
    }

    public List<MemberReferralEntity> findByReferrerId(UUID referrerId) {
        if (referrerId == null) {
            return List.of();
        }
        return executeListResultSearch(MemberReferralEntity.QUERY_FIND_BY_REFERRER_ID, Map.of("referrerId", referrerId));
    }

    public int deleteByRefereeId(UUID refereeId) {
        if (refereeId == null) {
            return 0;
        }
        return executeUpdateQuery(MemberReferralEntity.QUERY_DELETE_BY_REFEREE_ID, Map.of("refereeId", refereeId));
    }
}
