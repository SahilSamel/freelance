package com.freelance.configuration.repository.error;

import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.entity.error.ErrorConfiguration;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.*;

public class ErrorConfigurationRepository extends AbstractRepository<ErrorConfiguration, String> {

    public Map<String, ErrorConfigEntry> getAllErrorEntries() {
        List<ErrorConfiguration> list = executeListResultSearch(ErrorConfiguration.QUERY_FIND_ALL, null);
        Map<String, ErrorConfigEntry> map = new HashMap<>();
        for (ErrorConfiguration config : list) {
            map.put(config.getErrorId(), new ErrorConfigEntry(config.getStatusCode(), config.getErrorMessage()));
        }
        return map;
    }

    public Set<String> findAllIds() {
        List<String> ids = executeTypedListQuery(ErrorConfiguration.QUERY_FIND_ALL_IDS, String.class, null);
        return new HashSet<>(ids);
    }

    public int deleteSystemOwned(UUID systemUserId) {
        return executeUpdateQuery(ErrorConfiguration.QUERY_DELETE_SYSTEM_OWNED, Map.of("systemUserId", systemUserId));
    }

    public Map<String, ErrorConfiguration> findAllEntitiesAsMap() {
        List<ErrorConfiguration> list = executeListResultSearch(ErrorConfiguration.QUERY_FIND_ALL, null);
        Map<String, ErrorConfiguration> map = new HashMap<>();
        for (ErrorConfiguration config : list) {
            map.put(config.getErrorId(), config);
        }
        return map;
    }

    @Override
    public ErrorConfiguration get(String errorId) {
        return super.get(errorId);
    }
}
