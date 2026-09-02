package com.freelance.keys.configuration;

import com.freelance.config.IError;

/**
 * Enumerates client-facing validation errors for the configuration domain.
 */
public enum ValidationErrors implements IError {

    INVALID_CONFIG_ID(
            "FL_6001",
            "Configuration ID is required.",
            "Configuration ID is missing or blank",
            400
    ),

    INVALID_CONFIG_VALUE(
            "FL_6002",
            "Configuration value is required.",
            "Configuration value is null",
            400
    ),

    INVALID_ERROR_ID(
            "FL_6003",
            "Error ID is required.",
            "Error ID is missing or blank",
            400
    ),

    INVALID_STATUS_CODE(
            "FL_6004",
            "Status code must be a valid HTTP status code between 100 and 599.",
            "Status code out of valid HTTP range",
            400
    ),

    INVALID_ERROR_MESSAGE(
            "FL_6005",
            "Error message cannot be blank.",
            "Error message is blank",
            400
    ),

    CONFIG_VALUE_VALIDATION_FAILED(
            "FL_6006",
            "Configuration value does not match the required pattern.",
            "Configuration value failed regex validation pattern",
            400
    );

    private static final String CATEGORY = "configuration.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    ValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
