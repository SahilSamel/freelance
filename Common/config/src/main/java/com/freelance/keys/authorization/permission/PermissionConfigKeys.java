package com.freelance.keys.authorization.permission;

import com.freelance.config.IConfig;

public enum PermissionConfigKeys implements IConfig {

    AUTHORIZATION_ENABLED(
            "authorization.enabled",
            "true",
            "Controls whether role-based access control (RBAC) authorization verification is enabled",
            "^(true|false)$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    PermissionConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    PermissionConfigKeys(String key, String defaultValue, String description, String validation) {
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
