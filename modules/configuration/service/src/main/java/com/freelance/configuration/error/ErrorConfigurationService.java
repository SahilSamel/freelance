package com.freelance.configuration.error;

import com.freelance.cache.config.ErrorMessageCache;
import com.freelance.configuration.repository.error.ErrorConfigurationRepository;
import com.freelance.manager.CacheManager;

public class ErrorConfigurationService {

    ErrorConfigurationRepository repository;

    public ErrorConfigurationService(){
        this.repository = new ErrorConfigurationRepository();
    }

    public void preloadErrorCacheEntries() {
        CacheManager.getInstance().preload(ErrorMessageCache.NAME, repository.getAll());
    }

    public String loadErrorMessage(String errorId){
        return repository.get(errorId).getErrorMessage();
    }

}
