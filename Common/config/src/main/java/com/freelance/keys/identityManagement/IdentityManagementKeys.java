package com.freelance.keys.identityManagement;

import com.freelance.config.IConfig;

/**
 * Configuration keys for the Identity Management domain.
 */
public enum IdentityManagementKeys implements IConfig {

    /** Minimum character length permitted for a username. */
    USERNAME_MINIMUM_LENGTH(
            "identityManagement.username.minimumLength",
            "3",
            "Minimum allowed length for usernames"
    ),

    /** Maximum character length permitted for a username. */
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