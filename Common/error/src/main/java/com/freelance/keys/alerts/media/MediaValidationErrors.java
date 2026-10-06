package com.freelance.keys.alerts.media;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for media operations.
 */
public enum MediaValidationErrors implements IError {

    INVALID_MEDIA_DATA(
            "FL_7018",
            "Media upload requires non-empty base64 data.",
            "Media base64 data validation failed",
            400
    ),
    INVALID_MEDIA_ID(
            "FL_7019",
            "Media ID must be provided.",
            "Media ID validation failed",
            400
    ),
    MEDIA_PAYLOAD_TOO_LARGE(
            "FL_7020",
            "Media base64 payload exceeds maximum allowed size.",
            "Media base64 string exceeds maximum size",
            400
    );

    private static final String CATEGORY = "alerts.media.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    MediaValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
