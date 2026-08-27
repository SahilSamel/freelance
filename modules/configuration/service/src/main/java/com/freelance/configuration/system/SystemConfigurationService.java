package com.freelance.configuration.system;

import com.freelance.cache.config.SystemConfigurationCache;
import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.configuration.repository.system.SystemConfigurationRepository;
import com.freelance.manager.CacheManager;

/**
 * Service managing database lookups and cache preloading for dynamic application configuration properties.
 */
public class SystemConfigurationService {

    private final SystemConfigurationRepository repository;

    /**
     * Constructs a {@link SystemConfigurationService} with a default repository.
     */
    public SystemConfigurationService() {
        this(new SystemConfigurationRepository());
    }

    /**
     * Constructs a {@link SystemConfigurationService} with an injected repository.
     *
     * @param repository system configuration repository instance
     */
    public SystemConfigurationService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    /**
     * Preloads all system configuration key-value properties from the database into cache.
     */
    public void preloadSystemCacheEntries() {
        CacheManager.getInstance().preload(SystemConfigurationCache.NAME, repository.getAll());
    }

    /**
     * Loads the raw string value of a configuration property from the database.
     *
     * @param configId configuration property key string
     * @return property value string, or {@code null} if not found in database
     */
    public String loadConfigValue(String configId) {
        SystemConfiguration entity = repository.get(configId);
        return entity != null ? entity.getConfigValue() : null;
    }
}
