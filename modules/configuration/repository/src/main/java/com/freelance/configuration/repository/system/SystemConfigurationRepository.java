package com.freelance.configuration.repository.system;

import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemConfigurationRepository extends AbstractRepository <SystemConfiguration, String> {

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

    public SystemConfiguration get (String configId){
        return super.get(configId);
    }
}
