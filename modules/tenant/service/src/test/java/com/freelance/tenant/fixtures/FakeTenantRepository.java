package com.freelance.tenant.fixtures;

import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.repository.TenantRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeTenantRepository extends TenantRepository {

    private final Map<String, TenantEntity> storage = new HashMap<>();

    public FakeTenantRepository() {
        super();
    }

    public void save(TenantEntity entity) {
        if (entity != null && entity.getTenantId() != null) {
            storage.put(entity.getTenantId().toLowerCase(), entity);
        }
    }

    @Override
    public Optional<TenantEntity> findByTenantId(String tenantId) {
        if (tenantId == null) return Optional.empty();
        return Optional.ofNullable(storage.get(tenantId.toLowerCase()));
    }

    @Override
    public void persist(TenantEntity entity) {
        save(entity);
    }
}
