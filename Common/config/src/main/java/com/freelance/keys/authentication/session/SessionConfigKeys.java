package com.freelance.keys.authentication.session;

import com.freelance.config.IConfig;

public enum SessionConfigKeys implements IConfig {

    SESSION_TTL_SECONDS(
            "authentication.session.ttlSeconds",
            "1200",
            "Session time-to-live in seconds",
            "^[1-9][0-9]*$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    SessionConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    SessionConfigKeys(String key, String defaultValue, String description, String validation) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
        this.validation = validation;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getValidation() {
        return validation;
    }
}
