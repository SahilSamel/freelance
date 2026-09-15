package com.freelance.keys.audit;

import com.freelance.config.IConfig;

public enum AuditConfigKeys implements IConfig {

    AUDITING_ENABLED(
            "audit.enabled",
            "true",
            "Controls whether HTTP and system API audit logging is enabled",
            "^(true|false)$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    AuditConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    AuditConfigKeys(String key, String defaultValue, String description, String validation) {
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
