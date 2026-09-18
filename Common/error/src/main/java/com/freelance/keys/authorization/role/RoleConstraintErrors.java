package com.freelance.keys.authorization.role;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for roles in the authorization domain.
 */
public enum RoleConstraintErrors implements IError {

    ROLE_ALREADY_EXISTS(
            "FL_4102",
            "A role with this name already exists.",
            "Raised when attempting to create or rename a role to an existing name",
            409
    ),
    ROLE_NOT_FOUND(
            "FL_4103",
            "The requested role was not found.",
            "Raised when a role could not be found",
            404
    ),
    ROLE_TO_UPDATE_NOT_FOUND(
            "FL_4104",
            "The role you are trying to update does not exist.",
            "Raised when the target role for an update operation does not exist",
            404
    ),
    ROLE_TO_DELETE_NOT_FOUND(
            "FL_4105",
            "The role you are trying to delete does not exist.",
            "Raised when the target role for a delete operation does not exist",
            404
    ),
    SYSTEM_ROLE_CANNOT_BE_MANAGED(
            "FL_4106",
            "System roles cannot be modified or deleted.",
            "Raised when an attempt is made to update or delete a system-defined role",
            400
    );

    private static final String CATEGORY = "authorization.role.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    RoleConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
