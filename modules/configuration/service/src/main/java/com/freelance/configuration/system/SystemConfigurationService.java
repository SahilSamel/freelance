package com.freelance.configuration.system;

import com.freelance.cache.config.SystemConfigurationCache;
import com.freelance.configuration.repository.system.SystemConfigurationRepository;
import com.freelance.manager.CacheManager;

public class SystemConfigurationService {

    SystemConfigurationRepository repository;

    public SystemConfigurationService(){
        this.repository = new SystemConfigurationRepository();
    }

    public void preloadSystemCacheEntries() {
        CacheManager.getInstance().preload(SystemConfigurationCache.NAME, repository.getAll());
    }

    public String loadConfigValue(String configId) {
        return repository.get(configId).getConfigValue();
    }

}
