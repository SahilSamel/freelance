package com.freelance.keys.authorization;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for the authorization domain.
 * <p>
 * Implements {@link IError} to provide standardized 401 Unauthorized error codes,
 * messages, and descriptions when incoming authorization headers or security context
 * metadata are malformed or absent.
 * </p>
 */
public enum ValidationErrors implements IError {

    /** Raised when the security context or authorization header is absent or invalid. */
    UNAUTHORIZED(
            "FL_4001",
            "Authorization context is missing or invalid.",
            "Missing authorization context in request",
            401
    );

    private static final String CATEGORY = "authorization.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    /**
     * Constructs an authorization validation error definition.
     *
     * @param indirectionCode   opaque client-facing error code (e.g., "FL_4001")
     * @param defaultMessage    default client-facing error message
     * @param description       administrative description explaining trigger conditions
     * @param defaultStatusCode default HTTP status code (typically 401)
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
     * @return dot-separated hierarchical key (e.g., "authorization.validation.UNAUTHORIZED")
     */
    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    /**
     * Retrieves the unique opaque client-facing indirection error code.
     *
     * @return opaque error code string (e.g., "FL_4001")
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
