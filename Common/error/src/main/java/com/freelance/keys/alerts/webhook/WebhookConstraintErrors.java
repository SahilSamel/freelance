package com.freelance.keys.alerts.webhook;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for webhook processing.
 */
public enum WebhookConstraintErrors implements IError {

    WEBHOOK_SIGNATURE_INVALID(
            "FL_7106",
            "The HMAC SHA-256 signature in X-Hub-Signature-256 header is invalid.",
            "Meta webhook security handshake verification failed",
            401
    );

    private static final String CATEGORY = "alerts.webhook.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    WebhookConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
