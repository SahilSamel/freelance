package com.freelance.configuration.repository.system;

import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.rest.app.repository.AbstractRepository;
import com.freelance.system.SystemHardcoding;

import java.util.*;

public class SystemConfigurationRepository extends AbstractRepository<SystemConfiguration, String> {

    public Map<String, String> getAll() {
        List<SystemConfiguration> list = executeListResultSearch(SystemConfiguration.QUERY_FIND_ALL, null);
        Map<String, String> map = new HashMap<>();
        for (SystemConfiguration config : list) {
            map.put(config.getConfigId(), config.getConfigValue());
        }
        return map;
    }

    public Set<String> findAllIds() {
        List<String> ids = executeTypedListQuery(SystemConfiguration.QUERY_FIND_ALL_IDS, String.class, null);
        return new HashSet<>(ids);
    }

    public int deleteSystemOwned() {
        UUID systemUUID = SystemHardcoding.SYSTEM_USER_ID.asUuid();
        return executeUpdateQuery(SystemConfiguration.QUERY_DELETE_SYSTEM_OWNED, Map.of("systemUserId", systemUUID));
    }

    public Map<String, SystemConfiguration> findAllEntitiesAsMap() {
        List<SystemConfiguration> list = executeListResultSearch(SystemConfiguration.QUERY_FIND_ALL, null);
        Map<String, SystemConfiguration> map = new HashMap<>();
        for (SystemConfiguration config : list) {
            map.put(config.getConfigId(), config);
        }
        return map;
    }

    @Override
    public SystemConfiguration get(String configId) {
        return super.get(configId);
    }
}
