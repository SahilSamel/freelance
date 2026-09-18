package com.freelance.keys.authorization.role;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for roles in the authorization domain.
 */
public enum RoleValidationErrors implements IError {

    INVALID_ROLE_ID(
            "FL_4002",
            "Role ID must be a valid non-null identifier.",
            "Raised when a role identifier is null or invalid",
            400
    ),
    INVALID_ROLE_NAME(
            "FL_4003",
            "Role name is required.",
            "Raised when a role name is null or blank",
            400
    ),
    ROLE_NAME_TOO_SHORT(
            "FL_4004",
            "Role name is too short.",
            "Raised when role name length is below minimum",
            400
    ),
    ROLE_NAME_TOO_LONG(
            "FL_4005",
            "Role name is too long.",
            "Raised when role name length exceeds maximum",
            400
    ),
    ROLE_NAME_HAS_INVALID_CHARS(
            "FL_4006",
            "Role name contains invalid characters.",
            "Raised when role name does not match allowed pattern",
            400
    );

    private static final String CATEGORY = "authorization.role.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    RoleValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
