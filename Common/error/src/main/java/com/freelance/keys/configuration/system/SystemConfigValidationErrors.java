package com.freelance.keys.configuration.system;

import com.freelance.config.IError;

/**
 * Enumerates client-facing validation errors for system configuration management.
 */
public enum SystemConfigValidationErrors implements IError {

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
    CONFIG_VALUE_VALIDATION_FAILED(
            "FL_6006",
            "Configuration value does not match the required pattern.",
            "Configuration value failed regex validation pattern",
            400
    );

    private static final String CATEGORY = "configuration.system.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    SystemConfigValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
