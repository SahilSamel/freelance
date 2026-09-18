package com.freelance.keys.authentication.session;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for user sessions.
 */
public enum SessionConstraintErrors implements IError {

    INVALID_SESSION(
            "FL_2103",
            "The session is invalid or has been terminated.",
            "Session validation failed",
            400
    ),
    EXPIRED_SESSION(
            "FL_2104",
            "The session has expired. Please log in again.",
            "Session expiration occurred",
            401
    );

    private static final String CATEGORY = "authentication.session.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    SessionConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
