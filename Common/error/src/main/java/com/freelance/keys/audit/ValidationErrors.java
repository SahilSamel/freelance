package com.freelance.keys.audit;

import com.freelance.config.IBadRequestError;

/**
 * Validation error constants for the auditing domain.
 */
public enum ValidationErrors implements IBadRequestError {

    MISSING_HTTP_METHOD(
            "FL_5001",
            "HTTP method is required.",
            "Missing or blank HTTP method"
    ),
    MISSING_ENDPOINT(
            "FL_5002",
            "Endpoint path is required.",
            "Missing or blank endpoint"
    ),
    MISSING_STATUS_CODE(
            "FL_5003",
            "HTTP status code is required.",
            "Missing or invalid status code"
    ),
    MISSING_TIMESTAMP(
            "FL_5004",
            "Timestamp is required.",
            "Missing timestamp"
    ),
    INVALID_DURATION(
            "FL_5005",
            "Duration must be greater than or equal to zero.",
            "Duration is negative or invalid"
    ),
    MISSING_TRACE_ID(
            "FL_5006",
            "Trace ID is required.",
            "Missing or blank trace ID"
    );

    private static final String CATEGORY = "audit.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;

    ValidationErrors(String indirectionCode, String defaultMessage, String description) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
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
}
