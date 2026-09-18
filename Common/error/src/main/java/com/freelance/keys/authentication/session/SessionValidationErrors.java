package com.freelance.keys.authentication.session;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for user sessions.
 */
public enum SessionValidationErrors implements IError {

    INVALID_SESSION_KEY(
            "FL_2004",
            "The session key is missing or invalid.",
            "Session key validation failed",
            400
    ),
    UNABLE_TO_CREATE_SESSION(
            "FL_2005",
            "Cannot create user session due to invalid user context.",
            "Session creation failure",
            400
    );

    private static final String CATEGORY = "authentication.session.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    SessionValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
