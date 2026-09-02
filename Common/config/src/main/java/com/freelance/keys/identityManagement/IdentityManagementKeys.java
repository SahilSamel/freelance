package com.freelance.keys.identityManagement;

import com.freelance.config.IConfig;

public enum IdentityManagementKeys implements IConfig {

    USERNAME_MINIMUM_LENGTH(
            "identityManagement.username.minimumLength",
            "3",
            "Minimum allowed length for usernames"
    ),

    USERNAME_MAXIMUM_LENGTH(
            "identityManagement.username.maximumLength",
            "50",
            "Maximum allowed length for usernames"
    ),

    PASSWORD_MINIMUM_LENGTH(
            "identityManagement.password.minimumLength",
            "6",
            "Minimum allowed length for passwords"
    ),

    USERNAME_PATTERN(
            "identityManagement.username.pattern",
            "^[a-zA-Z0-9_.-]+$",
            "Regex pattern defining allowed characters for usernames"
    );

    private final String key;
    private final String defaultValue;
    private final String description;

    IdentityManagementKeys(String key, String defaultValue, String description) {
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