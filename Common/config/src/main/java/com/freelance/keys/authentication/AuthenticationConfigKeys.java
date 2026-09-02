package com.freelance.keys.authentication;

import com.freelance.config.IConfig;

public enum AuthenticationConfigKeys implements IConfig {

    SESSION_TTL_SECONDS(
            "authentication.session.ttlSeconds",
            "86400",
            "Session time-to-live in seconds",
            "^[1-9][0-9]*$"
    ),

    CONCURRENT_LOGIN_ALLOWED(
            "authentication.concurrentLoginAllowed",
            "true",
            "Whether multiple concurrent logins are allowed for the same user",
            "^(true|false)$"
    ),

    JWT_SECRET_KEY(
            "authentication.jwt.secret",
            "default-jwt-secret-key-that-is-at-least-32-bytes-long!",
            "Secret key used to sign and verify JWT tokens",
            "^.{32,}$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    AuthenticationConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    AuthenticationConfigKeys(String key, String defaultValue, String description, String validation) {
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