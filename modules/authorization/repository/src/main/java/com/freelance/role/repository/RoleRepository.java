package com.freelance.role.repository;

import com.freelance.role.entity.Role;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.*;

public class RoleRepository extends AbstractRepository<Role, UUID> {

    public RoleRepository() {
        super();
    }

    public Optional<Role> findById(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(get(id));
    }

    public Role findByName(String name) {
        if (name == null || name.isBlank()) return null;
        return executeSingleResultSearch(Role.QUERY_FIND_BY_NAME, Map.of("name", name)).orElse(null);
    }

    public boolean existsByName(String name) {
        if (name == null || name.isBlank()) return false;
        return findByName(name) != null;
    }

    public List<Role> findAll() {
        return executeListResultSearch(Role.QUERY_FIND_ALL, null);
    }

    public List<Role> findAllSystemRoles() {
        return executeListResultSearch(Role.QUERY_FIND_ALL_SYSTEM_ROLES, null);
    }

    public Map<String, Role> findAllSystemRolesAsMap() {
        List<Role> list = findAllSystemRoles();
        Map<String, Role> map = new HashMap<>();
        for (Role role : list) {
            map.put(role.getName(), role);
        }
        return map;
    }

    public int deleteSystemRoles() {
        return executeUpdateQuery(Role.QUERY_DELETE_SYSTEM_ROLES, null);
    }
}
