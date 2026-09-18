package com.freelance.keys.alerts.webhook;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for webhook payloads.
 */
public enum WebhookValidationErrors implements IError {

    INVALID_WEBHOOK_PAYLOAD(
            "FL_7005",
            "The received WhatsApp webhook payload is malformed or missing required structures.",
            "Webhook payload validation failed",
            400
    );

    private static final String CATEGORY = "alerts.webhook.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    WebhookValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
        this.defaultStatusCode = defaultStatusCode;
    }

    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    @Override
    public String getIndirectionCode() {
        return this.indirectionCode;
    }

    @Override
    public String getDefaultMessage() {
        return this.defaultMessage;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public int getDefaultStatusCode() {
        return this.defaultStatusCode;
    }
}
