package com.freelance.keys.authorization;

import com.freelance.config.IError;

/**
 * Enumerates business rule constraint violation errors for the authorization domain.
 * <p>
 * Implements {@link IError} to provide standardized 403 Forbidden error codes,
 * messages, and descriptions when an authenticated caller attempts an action without
 * holding the required role or permission key.
 * </p>
 */
public enum ConstraintErrors implements IError {

    /** Raised when the caller does not possess the requisite role or permission key. */
    UNAUTHORIZED(
            "FL_4101",
            "You do not have permission to perform this action.",
            "Caller lacks required role or permission",
            403
    ),
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
    ),
    AUTHORIZATION_NOT_FOUND(
            "FL_4107",
            "One or more specified authorizations do not exist.",
            "Raised when assigned authorization identifiers are not found in the database",
            400
    );

    private static final String CATEGORY = "authorization.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    /**
     * Constructs an authorization constraint error definition.
     *
     * @param indirectionCode   opaque client-facing error code (e.g., "FL_4101")
     * @param defaultMessage    default client-facing error message
     * @param description       administrative description explaining trigger conditions
     * @param defaultStatusCode default HTTP status code (typically 403)
     */
    ConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
        this.defaultStatusCode = defaultStatusCode;
    }

    /**
     * Retrieves the hierarchical dot-separated error key.
     *
     * @return dot-separated hierarchical key (e.g., "authorization.constraint.UNAUTHORIZED")
     */
    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    /**
     * Retrieves the unique opaque client-facing indirection error code.
     *
     * @return opaque error code string (e.g., "FL_4101")
     */
    @Override
    public String getIndirectionCode() {
        return this.indirectionCode;
    }

    /**
     * Retrieves the fallback localized error message.
     *
     * @return default user-facing message string
     */
    @Override
    public String getDefaultMessage() {
        return this.defaultMessage;
    }

    /**
     * Retrieves the human-readable administrative description.
     *
     * @return trigger description
     */
    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * Retrieves the canonical default HTTP status code.
     *
     * @return HTTP status code integer
     */
    @Override
    public int getDefaultStatusCode() {
        return this.defaultStatusCode;
    }
}
