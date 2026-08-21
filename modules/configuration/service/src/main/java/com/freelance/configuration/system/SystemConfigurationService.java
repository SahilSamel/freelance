package com.freelance.configuration.system;

import com.freelance.cache.config.SystemConfigurationCache;
import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.configuration.repository.system.SystemConfigurationRepository;
import com.freelance.manager.CacheManager;

public class SystemConfigurationService {

    private final SystemConfigurationRepository repository;

    public SystemConfigurationService() {
        this(new SystemConfigurationRepository());
    }

    public SystemConfigurationService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    public void preloadSystemCacheEntries() {
        CacheManager.getInstance().preload(SystemConfigurationCache.NAME, repository.getAll());
    }

    public String loadConfigValue(String configId) {
        SystemConfiguration entity = repository.get(configId);
        return entity != null ? entity.getConfigValue() : null;
    }
}
