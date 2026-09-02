package com.freelance.keys.authorization;

import com.freelance.config.IConfig;

public enum AuthorizationConfigKeys implements IConfig {

    ROLE_NAME_MINIMUM_LENGTH(
            "authorization.role.minimumLength",
            "3",
            "Minimum allowed length for role names"
    ),

    ROLE_NAME_MAXIMUM_LENGTH(
            "authorization.role.maximumLength",
            "50",
            "Maximum allowed length for role names"
    ),

    ROLE_NAME_PATTERN(
            "authorization.role.pattern",
            "^[a-zA-Z0-9_.-]+$",
            "Regex pattern defining allowed characters for role names"
    );

    private final String key;
    private final String defaultValue;
    private final String description;

    AuthorizationConfigKeys(String key, String defaultValue, String description) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
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
}
