package com.freelance.keys.audit.http;

import com.freelance.config.IConfig;

public enum HttpAuditConfigKeys implements IConfig {

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

    HttpAuditConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    HttpAuditConfigKeys(String key, String defaultValue, String description, String validation) {
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
