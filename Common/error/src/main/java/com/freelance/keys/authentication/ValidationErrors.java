package com.freelance.keys.authentication;

import com.freelance.config.IBadRequestError;

public enum ValidationErrors implements IBadRequestError {

    INVALID_USERNAME(
            "FL_2001",
            "The provided username is invalid or does not meet length requirements.",
            "Username validation failed"
    ),
    INVALID_PASSWORD(
            "FL_2002",
            "The provided password is blank or invalid.",
            "Password validation failed"
    ),
    INVALID_TOKEN(
            "FL_2003",
            "The provided authentication token is invalid or malformed.",
            "Token validation failed"
    ),
    INVALID_SESSION_KEY(
            "FL_2004",
            "The session key is missing or invalid.",
            "Session key validation failed"
    ),
    UNABLE_TO_CREATE_SESSION(
            "FL_2005",
            "Cannot create user session due to invalid user context.",
            "Session creation failure"
    );

    private static final String CATEGORY = "authentication.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;

    ValidationErrors(String indirectionCode, String defaultMessage, String description) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
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
}
