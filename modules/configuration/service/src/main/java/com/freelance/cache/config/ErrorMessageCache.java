package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.error.ErrorConfigurationService;

/**
 * Backs ConfigurationEngine's keys code and message lookups.
 */
public class ErrorMessageCache extends AbstractCacheConfig<String, ErrorConfigEntry> {

    public static final String NAME = "cache_ErrorMessage";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected ErrorConfigEntry load(String errorId) {
        ErrorConfigurationService errorConfigurationService = new ErrorConfigurationService();
        return errorConfigurationService.loadErrorConfig(errorId);
    }
}