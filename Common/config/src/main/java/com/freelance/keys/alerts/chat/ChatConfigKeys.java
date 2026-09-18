package com.freelance.keys.alerts.chat;

import com.freelance.config.IConfig;

public enum ChatConfigKeys implements IConfig {

    ALERT_HUMAN_DELAY_HOURS(
            "alerts.humanDelayHours",
            "24",
            "Duration in hours to hold automated bot alerts while human staff is chatting with user",
            "^[1-9][0-9]*$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    ChatConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    ChatConfigKeys(String key, String defaultValue, String description, String validation) {
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
