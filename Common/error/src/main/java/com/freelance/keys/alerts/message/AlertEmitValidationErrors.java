package com.freelance.keys.alerts.message;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for dispatching / emitting alerts.
 */
public enum AlertEmitValidationErrors implements IError {

    INVALID_USER_IDS(
            "FL_7001",
            "The provided user IDs list is empty or contains invalid UUIDs.",
            "User IDs validation failed for alert emit",
            400
    ),
    INVALID_CHANNEL(
            "FL_7007",
            "The communication channel is invalid or missing.",
            "Channel validation failed",
            400
    );

    private static final String CATEGORY = "alerts.message.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    AlertEmitValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
