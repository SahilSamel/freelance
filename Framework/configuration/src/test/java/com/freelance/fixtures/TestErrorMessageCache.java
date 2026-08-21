package com.freelance.fixtures;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.configuration.dto.ErrorConfigEntry;

public class TestErrorMessageCache extends AbstractCacheConfig<String, ErrorConfigEntry> {

    @Override
    protected String name() {
        return "cache_ErrorMessage";
    }

    @Override
    protected ErrorConfigEntry load(String key) {
        return null;
    }
}
