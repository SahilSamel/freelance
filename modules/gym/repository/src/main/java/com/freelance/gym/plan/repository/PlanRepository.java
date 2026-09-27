package com.freelance.gym.plan.repository;

import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlanRepository extends AbstractRepository<PlanEntity, UUID> {

    public Optional<PlanEntity> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return executeSingleResultSearch(PlanEntity.QUERY_FIND_BY_NAME, Map.of("name", name.trim()));
    }

    public List<PlanEntity> findAllPlans() {
        return executeListResultSearch(PlanEntity.QUERY_FIND_ALL, Map.of());
    }

    public List<PlanEntity> findAllActivePlans() {
        return executeListResultSearch(PlanEntity.QUERY_FIND_ALL_ACTIVE, Map.of());
    }

    public List<PlanEntity> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return executeListResultSearch(PlanEntity.QUERY_FIND_BY_IDS, Map.of("ids", ids));
    }
}
