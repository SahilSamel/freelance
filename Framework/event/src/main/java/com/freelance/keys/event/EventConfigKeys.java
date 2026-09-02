package com.freelance.keys.event;

import com.freelance.config.IConfig;

public enum EventConfigKeys implements IConfig {

    EVENT_BROKER_TYPE(
            "event.broker.type",
            "IN_MEMORY",
            "Underlying message broker driver",
            "^(IN_MEMORY|REDIS)$"
    ),
    REDIS_HOST(
            "event.redis.host",
            "localhost",
            "Redis server hostname or IP address"
    ),
    REDIS_PORT(
            "event.redis.port",
            "6379",
            "Redis server port number",
            "^[0-9]{1,5}$"
    ),
    REDIS_PASSWORD(
            "event.redis.password",
            "",
            "Redis server authentication password"
    ),
    REDIS_DATABASE(
            "event.redis.database",
            "0",
            "Redis database index",
            "^[0-9]+$"
    ),
    EVENT_SHUTDOWN_TIMEOUT_SECONDS(
            "event.shutdown.timeout_seconds",
            "10",
            "Graceful shutdown timeout in seconds for in-flight event tasks",
            "^[1-9][0-9]*$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    EventConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    EventConfigKeys(String key, String defaultValue, String description, String validation) {
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
