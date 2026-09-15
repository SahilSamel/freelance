package com.freelance.keys.alerts;

import com.freelance.config.IConfig;

public enum AlertConfigKeys implements IConfig {

    WHATSAPP_API_BASE_URL(
            "alerts.whatsapp.apiBaseUrl",
            "https://graph.facebook.com/v20.0",
            "Base URL for Meta WhatsApp Cloud API endpoint",
            "^https?://.+$"
    ),

    WHATSAPP_PHONE_NUMBER_ID(
            "alerts.whatsapp.phoneNumberId",
            "100000000000000",
            "Meta WhatsApp Business Phone Number ID",
            "^[0-9]+$"
    ),

    WHATSAPP_BUSINESS_ACCOUNT_ID(
            "alerts.whatsapp.businessAccountId",
            "100000000000001",
            "Meta WhatsApp Business Account ID (WABA ID) for template management",
            "^[0-9]+$"
    ),

    WHATSAPP_ACCESS_TOKEN(
            "alerts.whatsapp.accessToken",
            "default_placeholder_whatsapp_system_user_token",
            "Permanent System User Access Token for Meta WhatsApp Graph API",
            "^.+$"
    ),

    WHATSAPP_WEBHOOK_VERIFY_TOKEN(
            "alerts.whatsapp.webhookVerifyToken",
            "default_freelance_webhook_verify_secret_token",
            "Verification token configured on Meta App Webhooks",
            "^.+$"
    ),

    WHATSAPP_APP_SECRET(
            "alerts.whatsapp.appSecret",
            "default_meta_app_secret_for_hmac_sha256_verification",
            "Meta App Secret used to compute HMAC SHA-256 signatures",
            "^.+$"
    ),

    ALERT_HUMAN_DELAY_HOURS(
            "alerts.humanDelayHours",
            "24",
            "Duration in hours to hold automated bot alerts while human staff is chatting with user",
            "^[1-9][0-9]*$"
    ),

    ALERT_OUTBOUND_RATE_LIMIT(
            "alerts.outboundRateLimit",
            "50.0",
            "Maximum outbound WhatsApp messages per second to prevent API throttling",
            "^[0-9]+(\\.[0-9]+)?$"
    ),

    DEFAULT_ALERT_CHANNEL(
            "alerts.defaultChannel",
            "WHATSAPP",
            "Default communication channel used for alert templates, messages, and staff replies when unspecified",
            "^WHATSAPP$"
    ),

    WHATSAPP_OPT_OUT_KEYWORDS(
            "alerts.whatsapp.optOutKeywords",
            "STOP, STOPALL, UNSUBSCRIBE, CANCEL, END, QUIT, OPTOUT, OPT OUT, OPT-OUT, REVOKE, OFF",
            "Comma-separated list of inbound WhatsApp keywords that trigger user opt-out",
            "^.+$"
    ),

    WHATSAPP_OPT_IN_KEYWORDS(
            "alerts.whatsapp.optInKeywords",
            "START, UNSTOP, SUBSCRIBE, OPTIN, OPT IN, OPT-IN, RESUME, CONTINUE, RESTART, YES, ON",
            "Comma-separated list of inbound WhatsApp keywords that trigger user opt-in",
            "^.+$"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    AlertConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    AlertConfigKeys(String key, String defaultValue, String description, String validation) {
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