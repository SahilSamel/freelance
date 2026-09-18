package com.freelance.keys.alerts.message;

import com.freelance.config.IConfig;

public enum AlertEmitConfigKeys implements IConfig {

    DEFAULT_ALERT_CHANNEL(
            "alerts.defaultChannel",
            "WHATSAPP",
            "Default communication channel used for alert templates, messages, and staff replies when unspecified",
            "^WHATSAPP$"
    ),

    ALERT_OUTBOUND_RATE_LIMIT(
            "alerts.outboundRateLimit",
            "50.0",
            "Maximum outbound WhatsApp messages per second to prevent API throttling",
            "^[0-9]+(\\.[0-9]+)?$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    AlertEmitConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    AlertEmitConfigKeys(String key, String defaultValue, String description, String validation) {
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
