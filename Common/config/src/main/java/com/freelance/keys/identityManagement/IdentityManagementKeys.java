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
