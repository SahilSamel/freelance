package com.freelance.tenant.repository;

import com.freelance.rest.app.repository.AbstractRepository;
import com.freelance.tenant.entity.TenantEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TenantRepository extends AbstractRepository<TenantEntity, String> {

    public Optional<TenantEntity> findByTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        return executeSingleResultSearch(TenantEntity.QUERY_FIND_BY_TENANT_ID, Map.of("tenantId", tenantId.trim().toLowerCase()));
    }

    public List<TenantEntity> findAllTenants() {
        return executeListResultSearch(TenantEntity.QUERY_FIND_ALL, Map.of());
    }

    public List<TenantEntity> findAllActiveTenants() {
        return executeListResultSearch(TenantEntity.QUERY_FIND_ALL_ACTIVE, Map.of());
    }

    public boolean existsByTenantId(String tenantId) {
        return findByTenantId(tenantId).isPresent();
    }
}
