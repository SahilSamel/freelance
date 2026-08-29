package com.freelance.keys.configuration;

import com.freelance.config.IError;

/**
 * Enumerates business constraint errors for the configuration domain.
 */
public enum ConstraintErrors implements IError {

    CONFIGURATION_NOT_FOUND(
            "FL_6101",
            "The requested configuration was not found.",
            "Raised when updating a non-existent system configuration",
            404
    ),

    ERROR_CONFIGURATION_NOT_FOUND(
            "FL_6102",
            "The requested error configuration was not found.",
            "Raised when updating a non-existent error message configuration",
            404
    );

    private static final String CATEGORY = "configuration.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    ConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
