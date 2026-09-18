package com.freelance.keys.configuration.system;

import com.freelance.config.IError;

/**
 * Enumerates business constraint errors for system configuration management.
 */
public enum SystemConfigConstraintErrors implements IError {

    CONFIGURATION_NOT_FOUND(
            "FL_6101",
            "The requested configuration was not found.",
            "Raised when updating a non-existent system configuration",
            404
    );

    private static final String CATEGORY = "configuration.system.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    SystemConfigConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
