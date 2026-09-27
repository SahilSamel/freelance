package com.freelance.gym.payment.repository;

import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserPlanPaymentRepository extends AbstractRepository<UserPlanPaymentEntity, UUID> {

    public List<UserPlanPaymentEntity> findByUserPlanId(UUID userPlanId) {
        if (userPlanId == null) {
            return List.of();
        }
        return executeListResultSearch(UserPlanPaymentEntity.QUERY_FIND_BY_USER_PLAN_ID, Map.of("userPlanId", userPlanId));
    }

    public List<UserPlanPaymentEntity> findByPaymentId(UUID paymentId) {
        if (paymentId == null) {
            return List.of();
        }
        return executeListResultSearch(UserPlanPaymentEntity.QUERY_FIND_BY_PAYMENT_ID, Map.of("paymentId", paymentId));
    }

    public List<UserPlanPaymentEntity> findByUserPlanIds(List<UUID> userPlanIds) {
        if (userPlanIds == null || userPlanIds.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(UserPlanPaymentEntity.QUERY_FIND_BY_USER_PLAN_IDS, Map.of("userPlanIds", userPlanIds));
    }

    public List<UserPlanPaymentEntity> findByPaymentIds(List<UUID> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(UserPlanPaymentEntity.QUERY_FIND_BY_PAYMENT_IDS, Map.of("paymentIds", paymentIds));
    }

    public int deleteByUserPlanId(UUID userPlanId) {
        if (userPlanId == null) {
            return 0;
        }
        return executeUpdateQuery(UserPlanPaymentEntity.QUERY_DELETE_BY_USER_PLAN_ID, Map.of("userPlanId", userPlanId));
    }

    public int deleteByPaymentIds(List<UUID> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            return 0;
        }
        return executeUpdateQuery(UserPlanPaymentEntity.QUERY_DELETE_BY_PAYMENT_IDS, Map.of("paymentIds", paymentIds));
    }
}
