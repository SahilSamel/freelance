package com.freelance.authentication;

public enum AuthenticationConfigKeys {
    SESSION_TTL_SECONDS("authentication.session.ttlSeconds"),
    CONCURRENT_LOGIN_ALLOWED("authentication.concurrentLoginAllowed"),
    JWT_SECRET_KEY("authentication.jwt.secret");

    private final String key;

    AuthenticationConfigKeys(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}