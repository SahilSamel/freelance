package com.freelance.configuration.error;

import com.freelance.cache.config.ErrorMessageCache;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.entity.error.ErrorConfiguration;
import com.freelance.configuration.repository.error.ErrorConfigurationRepository;
import com.freelance.manager.CacheManager;

public class ErrorConfigurationService {

    private final ErrorConfigurationRepository repository;

    public ErrorConfigurationService() {
        this(new ErrorConfigurationRepository());
    }

    public ErrorConfigurationService(ErrorConfigurationRepository repository) {
        this.repository = repository;
    }

    public void preloadErrorCacheEntries() {
        CacheManager.getInstance().preload(ErrorMessageCache.NAME, repository.getAllErrorEntries());
    }

    public ErrorConfigEntry loadErrorConfig(String errorId) {
        ErrorConfiguration entity = repository.get(errorId);
        if (entity == null) {
            return null;
        }
        return new ErrorConfigEntry(entity.getStatusCode(), entity.getErrorMessage());
    }
}
