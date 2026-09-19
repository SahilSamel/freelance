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
    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        for (SystemConfiguration cfg : storage.values()) {
            map.put(cfg.getConfigId(), cfg.getConfigValue());
        }
        return map;
    }

    @Override
    public Set<String> findAllIds() {
        return new HashSet<>(storage.keySet());
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

    @Override
    public Map<String, SystemConfiguration> findAllEntitiesAsMap() {
        return new HashMap<>(storage);
    }
}
