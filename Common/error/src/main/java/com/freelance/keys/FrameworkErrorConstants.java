package com.freelance.keys;

import com.freelance.config.IError;

/**
 * Enumerates generic, framework-level error categories used as fallbacks
 * or defaults across the application (as opposed to domain-specific errors).
 */
public enum FrameworkErrorConstants implements IError {

    VALIDATION_ERROR(
            "FL_1001",
            "One or more validation constraints were violated.",
            "Generic validation failure",
            400
    ),
    CONSTRAINT_ERROR(
            "FL_1002",
            "A business constraint was violated.",
            "Generic constraint violation",
            400
    ),
    MISSING_CONFIGURATION_ERROR(
            "FL_1003",
            "A required system configuration is missing or invalid.",
            "Missing system configuration error",
            500
    ),
    INTERNAL_SERVER_ERROR(
            "FL_1004",
            "An unexpected internal server error occurred.",
            "Unexpected server-side failure",
            500
    );

    private static final String CATEGORY = "framework";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    FrameworkErrorConstants(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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