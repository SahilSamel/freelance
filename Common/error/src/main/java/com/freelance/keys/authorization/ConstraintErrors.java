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
