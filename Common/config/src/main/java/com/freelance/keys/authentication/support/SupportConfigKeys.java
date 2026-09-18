package com.freelance.keys.authentication.support;

import com.freelance.config.IConfig;

public enum SupportConfigKeys implements IConfig {

    VENDOR_SUPPORT_PUBLIC_KEY(
            "authentication.vendor.support.publicKey",
            "",
            "Base64-encoded X.509 RSA public key used to verify vendor support tokens"
    ),

    SUPPORT_ACCESS_DEFAULT_DURATION_HOURS(
            "authentication.support.access.defaultDurationHours",
            "2",
            "Default duration in hours for granted vendor support access windows (1-6 hours)",
            "^[1-6]$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    SupportConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    SupportConfigKeys(String key, String defaultValue, String description, String validation) {
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
