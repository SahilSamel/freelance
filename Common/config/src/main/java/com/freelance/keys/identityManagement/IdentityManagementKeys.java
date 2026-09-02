package com.freelance.keys.identityManagement;

import com.freelance.config.IConfig;

public enum IdentityManagementKeys implements IConfig {

    USERNAME_MINIMUM_LENGTH(
            "identityManagement.username.minimumLength",
            "3",
            "Minimum allowed length for usernames",
            "^[1-9][0-9]*$"
    ),

    USERNAME_MAXIMUM_LENGTH(
            "identityManagement.username.maximumLength",
            "50",
            "Maximum allowed length for usernames",
            "^[1-9][0-9]*$"
    ),

    PASSWORD_MINIMUM_LENGTH(
            "identityManagement.password.minimumLength",
            "6",
            "Minimum allowed length for passwords",
            "^[1-9][0-9]*$"
    ),

    USERNAME_PATTERN(
            "identityManagement.username.pattern",
            "^[a-zA-Z0-9_.-]+$",
            "Regex pattern defining allowed characters for usernames"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    IdentityManagementKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    IdentityManagementKeys(String key, String defaultValue, String description, String validation) {
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