package com.freelance.keys.authentication.auth;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for user authentication and credentials.
 */
public enum AuthConstraintErrors implements IError {

    INVALID_CREDENTIALS(
            "FL_2101",
            "The username or password provided is incorrect.",
            "Given credentials do not match identity records",
            400
    ),
    UNAUTHORIZED(
            "FL_2102",
            "Authentication is required to access this resource.",
            "Unauthorized access attempt",
            401
    );

    private static final String CATEGORY = "authentication.auth.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    AuthConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
