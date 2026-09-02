package com.freelance.keys.event;

import com.freelance.config.IConfig;

public enum EventConfigKeys implements IConfig {

    EVENT_BROKER_TYPE(
            "event.broker.type",
            "IN_MEMORY",
            "Underlying message broker driver"
    ),
    REDIS_HOST(
            "event.redis.host",
            "localhost",
            "Redis server hostname or IP address"
    ),
    REDIS_PORT(
            "event.redis.port",
            "6379",
            "Redis server port number"
    ),
    REDIS_PASSWORD(
            "event.redis.password",
            "",
            "Redis server authentication password"
    ),
    REDIS_DATABASE(
            "event.redis.database",
            "0",
            "Redis database index"
    ),
    EVENT_SHUTDOWN_TIMEOUT_SECONDS(
            "event.shutdown.timeout_seconds",
            "10",
            "Graceful shutdown timeout in seconds for in-flight event tasks"
    );

    private final String key;
    private final String defaultValue;
    private final String description;

    EventConfigKeys(String key, String defaultValue, String description) {
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
