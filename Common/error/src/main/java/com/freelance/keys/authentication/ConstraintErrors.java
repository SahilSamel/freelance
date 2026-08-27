package com.freelance.keys.authentication;

import com.freelance.config.IError;

/**
 * Constraint violation error keys for the Authentication domain.
 * <p>
 * Implements {@link IError} to declare business constraint errors such as invalid credentials,
 * unauthorized access attempts, and invalid/expired sessions.
 * </p>
 */
public enum ConstraintErrors implements IError {

    /** Error raised when supplied login credentials do not match stored identity records. */
    INVALID_CREDENTIALS(
            "FL_2101",
            "The username or password provided is incorrect.",
            "Given credentials do not match identity records",
            400
    ),

    /** Error raised when authentication is required to access a protected resource. */
    UNAUTHORIZED(
            "FL_2102",
            "Authentication is required to access this resource.",
            "Unauthorized access attempt",
            401
    ),

    /** Error raised when an active session token/key is invalid or has been terminated. */
    INVALID_SESSION(
            "FL_2103",
            "The session is invalid or has been terminated.",
            "Session validation failed",
            400
    ),

    /** Error raised when an established user session has expired beyond its time-to-live. */
    EXPIRED_SESSION(
            "FL_2104",
            "The session has expired. Please log in again.",
            "Session expiration occurred",
            401
    );

    private static final String CATEGORY = "authentication.constraint";

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
