package com.freelance.keys.alerts.media;

import com.freelance.config.IError;

/**
 * Enumerates client-facing domain constraint errors for media operations.
 */
public enum MediaConstraintErrors implements IError {

    MEDIA_NOT_FOUND(
            "FL_7113",
            "The requested media resource could not be found.",
            "Media lookup in local cache and upstream Meta failed",
            404
    ),
    MEDIA_UPLOAD_FAILED(
            "FL_7114",
            "Failed to upload media to WhatsApp Media API.",
            "Upstream WhatsApp Media API error",
            502
    ),
    MEDIA_SIZE_EXCEEDED(
            "FL_7115",
            "Media payload exceeds maximum allowed size of 16 MB.",
            "Media payload size exceeded maximum allowed limit",
            413
    );

    private static final String CATEGORY = "alerts.media.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    MediaConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
