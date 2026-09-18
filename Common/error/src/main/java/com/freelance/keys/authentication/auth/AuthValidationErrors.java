package com.freelance.keys.authentication.auth;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for user authentication and credentials.
 */
public enum AuthValidationErrors implements IError {

    INVALID_USERNAME(
            "FL_2001",
            "The provided username is invalid or does not meet length requirements.",
            "Username validation failed",
            400
    ),
    INVALID_PASSWORD(
            "FL_2002",
            "The provided password is blank or invalid.",
            "Password validation failed",
            400
    ),
    INVALID_TOKEN(
            "FL_2003",
            "The provided authentication token is invalid or malformed.",
            "Token validation failed",
            400
    );

    private static final String CATEGORY = "authentication.auth.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    AuthValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
