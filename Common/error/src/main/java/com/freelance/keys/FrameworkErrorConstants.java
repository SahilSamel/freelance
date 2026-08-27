package com.freelance.keys;

import com.freelance.config.IError;

/**
 * Enumerates framework-level error constants used as standardized defaults
 * and fallback failure codes across the application infrastructure.
 * <p>
 * Implements {@link IError} to supply unique indirection error codes, default
 * user-facing messages, administrative descriptions, and HTTP status codes.
 * </p>
 */
public enum FrameworkErrorConstants implements IError {

    /** Raised when one or more input field validation rules fail. */
    VALIDATION_ERROR(
            "FL_1001",
            "One or more validation constraints were violated.",
            "Generic validation failure",
            400
    ),

    /** Raised when a business rule or database integrity constraint is violated. */
    CONSTRAINT_ERROR(
            "FL_1002",
            "A business constraint was violated.",
            "Generic constraint violation",
            400
    ),

    /** Raised when an expected system configuration entry cannot be located or parsed. */
    MISSING_CONFIGURATION_ERROR(
            "FL_1003",
            "A required system configuration is missing or invalid.",
            "Missing system configuration error",
            500
    ),

    /** Raised when an unexpected runtime exception escapes the application request boundary. */
    INTERNAL_SERVER_ERROR(
            "FL_1004",
            "An unexpected internal server error occurred.",
            "Unexpected server-side failure",
            500
    );

    private static final String CATEGORY = "framework";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    /**
     * Constructs a framework error constant with the specified contract properties.
     *
     * @param indirectionCode   opaque client-facing error code (e.g., "FL_1001")
     * @param defaultMessage    fallback user-facing error message
     * @param description       administrative description explaining trigger conditions
     * @param defaultStatusCode default HTTP status code associated with this error
     */
    FrameworkErrorConstants(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
        this.defaultStatusCode = defaultStatusCode;
    }

    /**
     * Retrieves the hierarchical dot-separated error key.
     *
     * @return dot-separated hierarchical key (e.g., "framework.VALIDATION_ERROR")
     */
    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    /**
     * Retrieves the unique opaque client-facing indirection error code.
     *
     * @return opaque error code string (e.g., "FL_1001")
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
     * @return description explaining under what condition the error is triggered
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