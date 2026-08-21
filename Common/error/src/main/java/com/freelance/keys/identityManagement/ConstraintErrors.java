package com.freelance.keys.identityManagement;

import com.freelance.config.IBadRequestError;

/**
 * Enumerates client-facing constraint violation errors for the identity
 * management domain (e.g. duplicate users, weak passwords).
 */
public enum ConstraintErrors implements IBadRequestError {

    USER_ALREADY_EXISTS(
            "FL_3101",
            "A user with this username or email already exists.",
            "Raised when a user with the same identity already exists",
            409
    );

    private static final String CATEGORY = "identity.constraint";

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