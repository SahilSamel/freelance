package com.freelance.keys.authentication;

import com.freelance.config.IConfig;

/**
 * Configuration keys for the Authentication domain.
 */
public enum AuthenticationConfigKeys implements IConfig {

    /** Session time-to-live duration in seconds. */
    SESSION_TTL_SECONDS(
            "authentication.session.ttlSeconds",
            "86400",
            "Session time-to-live in seconds"
    ),

    /** Flag indicating whether multiple concurrent sessions are permitted for a single user. */
    CONCURRENT_LOGIN_ALLOWED(
            "authentication.concurrentLoginAllowed",
            "true",
            "Whether multiple concurrent logins are allowed for the same user"
    ),

    /** Secret key used to sign and verify HMAC-SHA JWT bearer tokens. */
    JWT_SECRET_KEY(
            "authentication.jwt.secret",
            "default-jwt-secret-key-that-is-at-least-32-bytes-long!",
            "Secret key used to sign and verify JWT tokens"
    );

    private final String key;
    private final String defaultValue;
    private final String description;

    AuthenticationConfigKeys(String key, String defaultValue, String description) {
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