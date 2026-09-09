package com.freelance.fixtures;

import com.freelance.authorization.entity.Authorization;
import com.freelance.authorization.repository.AuthorizationRepository;

import java.util.*;

public class FakeAuthorizationRepository extends AuthorizationRepository {
    public final Map<UUID, Authorization> storage = new HashMap<>();

    @Override
    public Authorization get(UUID id) {
        return storage.get(id);
    }

    @Override
    public List<Authorization> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Authorization findByKey(String key) {
        if (key == null) return null;
        return storage.values().stream()
                .filter(a -> key.equals(a.getKey()))
                .findFirst()
                .orElse(null);
    }
}
