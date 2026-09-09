package com.freelance.fixtures;

import com.freelance.configuration.system.entity.SystemConfiguration;
import com.freelance.configuration.system.repository.SystemConfigurationRepository;

import java.util.*;

public class FakeSystemConfigurationRepository extends SystemConfigurationRepository {
    public final Map<String, SystemConfiguration> storage = new HashMap<>();
    public int mergeCalls = 0;

    @Override
    public SystemConfiguration get(String configId) {
        return storage.get(configId);
    }

    @Override
    public List<SystemConfiguration> findAllEntities() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public SystemConfiguration merge(SystemConfiguration entity) {
        mergeCalls++;
        if (entity != null) {
            storage.put(entity.getConfigId(), entity);
        }
        return entity;
    }
}
