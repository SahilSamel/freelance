package com.freelance.gym.payment.repository;

import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PaymentRepository extends AbstractRepository<PaymentEntity, UUID> {

    public List<PaymentEntity> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(PaymentEntity.QUERY_FIND_BY_IDS, Map.of("ids", ids));
    }

    public int deleteByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return executeUpdateQuery(PaymentEntity.QUERY_DELETE_BY_IDS, Map.of("ids", ids));
    }

    public int deletePendingByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return executeUpdateQuery(PaymentEntity.QUERY_DELETE_PENDING_BY_IDS, Map.of("ids", ids));
    }
}
