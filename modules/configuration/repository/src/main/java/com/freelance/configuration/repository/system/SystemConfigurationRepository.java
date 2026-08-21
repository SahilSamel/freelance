package com.freelance.configuration.repository.system;

import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.rest.app.repository.AbstractRepository;
import com.freelance.system.SystemHardcoding;

import java.util.*;

public class SystemConfigurationRepository extends AbstractRepository<SystemConfiguration, String> {

    public Map<String, String> getAll() {
        List<SystemConfiguration> list = em()
                .createQuery("SELECT s FROM SystemConfiguration s", SystemConfiguration.class)
                .getResultList();

        Map<String, String> map = new HashMap<>();
        for (SystemConfiguration config : list) {
            map.put(config.getConfigId(), config.getConfigValue());
        }
        return map;
    }

    public Set<String> findAllIds() {
        List<String> ids = em().createQuery(
                        "SELECT s.configId FROM SystemConfiguration s", String.class)
                .getResultList();
        return new java.util.HashSet<>(ids);
    }

    public int deleteSystemOwned() {
        UUID systemUUID = SystemHardcoding.SYSTEM_USER_ID.asUuid();
        return em().createQuery("DELETE FROM SystemConfiguration s WHERE s.lastUpdatedBy IS NULL OR s.lastUpdatedBy = :systemUserId")
                .setParameter("systemUserId", systemUUID)
                .executeUpdate();
    }

    public SystemConfiguration get(String configId) {
        return super.get(configId);
    }
}
