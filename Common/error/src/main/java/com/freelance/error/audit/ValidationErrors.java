package com.freelance.error.audit;

import com.freelance.error.IBadRequestError;

/**
 * Validation error constants for the auditing domain.
 */
public enum ValidationErrors implements IBadRequestError {

    /**
     * Missing or blank HTTP method.
     */
    MISSING_HTTP_METHOD("MISSING_HTTP_METHOD"),

    /**
     * Missing or blank endpoint.
     */
    MISSING_ENDPOINT("MISSING_ENDPOINT"),

    /**
     * Missing or invalid status code.
     */
    MISSING_STATUS_CODE("MISSING_STATUS_CODE"),

    /**
     * Missing timestamp.
     */
    MISSING_TIMESTAMP("MISSING_TIMESTAMP"),

    /**
     * Duration is negative or invalid.
     */
    INVALID_DURATION("INVALID_DURATION"),

    /**
     * Missing or blank trace ID.
     */
    MISSING_TRACE_ID("MISSING_TRACE_ID");

    private static final String CATEGORY = "audit.validation";

    private final String code;

    ValidationErrors(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getConfigKey() {
        return CATEGORY + "." + this.code;
    }
}
