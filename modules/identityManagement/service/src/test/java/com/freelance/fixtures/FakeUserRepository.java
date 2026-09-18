package com.freelance.fixtures;

import com.freelance.user.entity.User;
import com.freelance.user.repository.UserRepository;

import java.util.*;

public class FakeUserRepository extends UserRepository {

    public final Map<UUID, User> storage = new HashMap<>();
    public int persistCalls = 0;
    public int mergeCalls = 0;
    public int removeCalls = 0;

    @Override
    public User get(UUID id) {
        return storage.get(id);
    }

    @Override
    public Optional<User> findById(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return storage.values().stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) return false;
        return storage.values().stream()
                .anyMatch(u -> username.equals(u.getUsername()));
    }

    @Override
    public long countUsers() {
        return storage.size();
    }

    @Override
    public void persist(User entity) {
        persistCalls++;
        if (entity != null) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            storage.put(entity.getId(), entity);
        }
    }

    @Override
    public User merge(User entity) {
        mergeCalls++;
        if (entity != null) {
            storage.put(entity.getId(), entity);
        }
        return entity;
    }

    @Override
    public void remove(User entity) {
        removeCalls++;
        if (entity != null) {
            storage.remove(entity.getId());
        }
    }
}
