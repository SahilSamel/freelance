package com.freelance.configuration.repository.error;

import com.freelance.configuration.entity.error.ErrorConfiguration;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorConfigurationRepository extends AbstractRepository<ErrorConfiguration, String> {

    public Map<String, String> getAll() {
        List<ErrorConfiguration> list = em()
                .createQuery("SELECT e FROM ErrorConfiguration e", ErrorConfiguration.class)
                .getResultList();

        Map<String, String> map = new HashMap<>();
        for (ErrorConfiguration config : list) {
            map.put(config.getErrorId(), config.getErrorMessage());
        }
        return map;
    }

    public ErrorConfiguration get(String errorId) {
        return super.get(errorId);
    }
}
