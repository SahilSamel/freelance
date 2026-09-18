package com.freelance.keys.authentication.auth;

import com.freelance.config.IConfig;

public enum AuthConfigKeys implements IConfig {

    CONCURRENT_LOGIN_ALLOWED(
            "authentication.concurrentLoginAllowed",
            "true",
            "Whether multiple concurrent logins are allowed for the same user",
            "^(true|false)$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    AuthConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    AuthConfigKeys(String key, String defaultValue, String description, String validation) {
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
