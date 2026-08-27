package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.configuration.system.SystemConfigurationService;

public class SystemConfigurationCache extends AbstractCacheConfig<String, String> {

    public static final String NAME = "cache_SystemConfig";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected String load(String configId) {
        SystemConfigurationService systemConfigurationService = new SystemConfigurationService();
        return systemConfigurationService.loadConfigValue(configId);
    }
}
