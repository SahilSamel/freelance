package com.freelance.repository.authorization;

import com.freelance.entity.authorization.Authorization;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.*;

public class AuthorizationRepository extends AbstractRepository<Authorization, UUID> {

    public AuthorizationRepository() {
        super();
    }

    public Set<String> findAllKeys() {
        List<String> keys = executeTypedListQuery(Authorization.QUERY_FIND_ALL_KEYS, String.class, null);
        return new HashSet<>(keys);
    }

    public Map<String, Authorization> findAllAsMap() {
        List<Authorization> list = executeListResultSearch(Authorization.QUERY_FIND_ALL, null);
        Map<String, Authorization> map = new HashMap<>();
        for (Authorization auth : list) {
            map.put(auth.getKey(), auth);
        }
        return map;
    }

    public Authorization findByKey(String key) {
        if (key == null) return null;
        return executeSingleResultSearch(Authorization.QUERY_FIND_BY_KEY, Map.of("key", key)).orElse(null);
    }
}
