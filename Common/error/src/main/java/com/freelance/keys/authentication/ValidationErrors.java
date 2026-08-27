package com.freelance.keys.authentication;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for the authentication domain.
 * <p>
 * Implements {@link IError} to provide standardized 400 Bad Request error codes,
 * messages, and descriptions for incoming authentication payloads.
 * </p>
 */
public enum ValidationErrors implements IError {

    /**
     * Raised when the supplied username is null, blank, or outside allowed length boundaries.
     */
    INVALID_USERNAME(
            "FL_2001",
            "The provided username is invalid or does not meet length requirements.",
            "Username validation failed",
            400
    ),

    /**
     * Raised when the supplied password is null or blank.
     */
    INVALID_PASSWORD(
            "FL_2002",
            "The provided password is blank or invalid.",
            "Password validation failed",
            400
    ),

    /**
     * Raised when the JWT authentication token is null, malformed, or unparseable.
     */
    INVALID_TOKEN(
            "FL_2003",
            "The provided authentication token is invalid or malformed.",
            "Token validation failed",
            400
    ),

    /**
     * Raised when the session key token is missing or malformed.
     */
    INVALID_SESSION_KEY(
            "FL_2004",
            "The session key is missing or invalid.",
            "Session key validation failed",
            400
    ),

    /**
     * Raised when a user session cannot be initialized due to invalid caller context.
     */
    UNABLE_TO_CREATE_SESSION(
            "FL_2005",
            "Cannot create user session due to invalid user context.",
            "Session creation failure",
            400
    );

    private static final String CATEGORY = "authentication.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    /**
     * Constructs an authentication validation error definition.
     *
     * @param indirectionCode   opaque client-facing error code (e.g., "FL_2001")
     * @param defaultMessage    default client-facing error message
     * @param description       administrative description explaining trigger conditions
     * @param defaultStatusCode default HTTP status code (typically 400)
     */
    ValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
        this.defaultStatusCode = defaultStatusCode;
    }

    /**
     * Retrieves the hierarchical dot-separated error key.
     *
     * @return dot-separated hierarchical key (e.g., "authentication.validation.INVALID_USERNAME")
     */
    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    /**
     * Retrieves the unique opaque client-facing indirection error code.
     *
     * @return opaque error code string (e.g., "FL_2001")
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
