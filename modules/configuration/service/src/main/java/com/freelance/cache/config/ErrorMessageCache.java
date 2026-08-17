package com.freelance.cache.config;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.configuration.error.ErrorConfigurationService;

/**
 * Backs ConfigurationEngine's error message lookups. The actual DB read
 * lives in ConfigurationEngine; this class only wires that lookup into
 * the cache.
 */
public class ErrorMessageCache extends AbstractCacheConfig<String, String> {

    public static final String NAME = "cache_ErrorMessage";

    @Override
    protected String name() {
        return NAME;
    }

    @Override
    protected String load(String errorId) {
        ErrorConfigurationService errorConfigurationService = new ErrorConfigurationService();
        return errorConfigurationService.loadErrorMessage(errorId);
    }
}