package com.freelance.configuration.repository.error;

import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.entity.error.ErrorConfiguration;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.*;

public class ErrorConfigurationRepository extends AbstractRepository<ErrorConfiguration, String> {

    public Map<String, ErrorConfigEntry> getAllErrorEntries() {
        List<ErrorConfiguration> list = em()
                .createQuery("SELECT e FROM ErrorConfiguration e", ErrorConfiguration.class)
                .getResultList();

        Map<String, ErrorConfigEntry> map = new HashMap<>();
        for (ErrorConfiguration config : list) {
            map.put(config.getErrorId(), new ErrorConfigEntry(config.getStatusCode(), config.getErrorMessage()));
        }
        return map;
    }

    public Set<String> findAllIds() {
        List<String> ids = em()
                .createQuery("SELECT e.errorId FROM ErrorConfiguration e", String.class)
                .getResultList();
        return new HashSet<>(ids);
    }

    public int deleteSystemOwned(UUID systemUserId) {
        return em().createQuery("DELETE FROM ErrorConfiguration e WHERE e.lastUpdatedBy IS NULL OR e.lastUpdatedBy = :systemUserId")
                .setParameter("systemUserId", systemUserId)
                .executeUpdate();
    }

    public ErrorConfiguration get(String errorId) {
        return super.get(errorId);
    }
}
