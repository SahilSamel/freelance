package com.freelance.fixtures;

import com.freelance.configuration.error.entity.ErrorConfiguration;
import com.freelance.configuration.error.repository.ErrorConfigurationRepository;

import java.util.*;

public class FakeErrorConfigurationRepository extends ErrorConfigurationRepository {
    public final Map<String, ErrorConfiguration> storage = new HashMap<>();
    public int mergeCalls = 0;

    @Override
    public ErrorConfiguration get(String errorId) {
        return storage.get(errorId);
    }

    @Override
    public List<ErrorConfiguration> findAllEntities() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public ErrorConfiguration merge(ErrorConfiguration entity) {
        mergeCalls++;
        if (entity != null) {
            storage.put(entity.getErrorId(), entity);
        }
        return entity;
    }
}
