package com.freelance.keys.authorization.permission;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for permissions/authorizations.
 */
public enum PermissionConstraintErrors implements IError {

    /** Raised when the caller does not possess the requisite role or permission key. */
    UNAUTHORIZED(
            "FL_4101",
            "You do not have permission to perform this action.",
            "Caller lacks required role or permission",
            403
    ),
    AUTHORIZATION_NOT_FOUND(
            "FL_4107",
            "One or more specified authorizations do not exist.",
            "Raised when assigned authorization identifiers are not found in the database",
            400
    );

    private static final String CATEGORY = "authorization.permission.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    PermissionConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
