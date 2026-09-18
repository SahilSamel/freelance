package com.freelance.keys.authorization.permission;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for permissions/security context in the authorization domain.
 */
public enum PermissionValidationErrors implements IError {

    /** Raised when the security context or authorization header is absent or invalid. */
    UNAUTHORIZED(
            "FL_4001",
            "Authorization context is missing or invalid.",
            "Missing authorization context in request",
            401
    );

    private static final String CATEGORY = "authorization.permission.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    PermissionValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
