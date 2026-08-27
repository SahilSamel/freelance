package com.freelance.repository.role;

import com.freelance.entity.role.Role;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.*;

public class RoleRepository extends AbstractRepository<Role, UUID> {

    public RoleRepository() {
        super();
    }

    public Role findByName(String name) {
        if (name == null) return null;
        return executeSingleResultSearch(Role.QUERY_FIND_BY_NAME, Map.of("name", name)).orElse(null);
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
