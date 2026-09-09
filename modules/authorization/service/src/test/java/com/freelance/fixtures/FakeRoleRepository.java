package com.freelance.fixtures;

import com.freelance.role.entity.Role;
import com.freelance.role.repository.RoleRepository;

import java.util.*;

public class FakeRoleRepository extends RoleRepository {
    public final Map<UUID, Role> storage = new HashMap<>();
    public int persistCalls = 0;
    public int mergeCalls = 0;
    public int removeCalls = 0;

    @Override
    public Optional<Role> findById(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Role findByName(String name) {
        if (name == null) return null;
        return storage.values().stream()
                .filter(r -> name.equals(r.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) return false;
        return storage.values().stream().anyMatch(r -> name.equals(r.getName()));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<Role> findAllSystemRoles() {
        return storage.values().stream().filter(Role::isSystem).toList();
    }

    @Override
    public void persist(Role entity) {
        persistCalls++;
        if (entity != null) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            storage.put(entity.getId(), entity);
        }
    }

    @Override
    public Role merge(Role entity) {
        mergeCalls++;
        if (entity != null) {
            storage.put(entity.getId(), entity);
        }
        return entity;
    }

    @Override
    public void remove(Role entity) {
        removeCalls++;
        if (entity != null) {
            storage.remove(entity.getId());
        }
    }
}
