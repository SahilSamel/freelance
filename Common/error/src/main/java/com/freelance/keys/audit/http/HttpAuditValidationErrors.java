package com.freelance.keys.audit.http;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for HTTP request and response auditing.
 */
public enum HttpAuditValidationErrors implements IError {

    MISSING_HTTP_METHOD(
            "FL_5001",
            "HTTP method is required.",
            "Missing or blank HTTP method",
            400
    ),
    MISSING_ENDPOINT(
            "FL_5002",
            "Endpoint path is required.",
            "Missing or blank endpoint",
            400
    ),
    MISSING_STATUS_CODE(
            "FL_5003",
            "HTTP status code is required.",
            "Missing or invalid status code",
            400
    ),
    MISSING_TIMESTAMP(
            "FL_5004",
            "Timestamp is required.",
            "Missing timestamp",
            400
    ),
    INVALID_DURATION(
            "FL_5005",
            "Duration must be greater than or equal to zero.",
            "Duration is negative or invalid",
            400
    ),
    MISSING_TRACE_ID(
            "FL_5006",
            "Trace ID is required.",
            "Missing or blank trace ID",
            400
    ),
    INVALID_LIMIT(
            "FL_5007",
            "Limit must be greater than zero.",
            "Pagination limit is non-positive or exceeds maximum allowed",
            400
    ),
    INVALID_OFFSET(
            "FL_5008",
            "Offset must be greater than or equal to zero.",
            "Pagination offset is negative",
            400
    ),
    INVALID_DATE_RANGE(
            "FL_5009",
            "Start timestamp cannot be after end timestamp.",
            "fromTimestamp is greater than toTimestamp",
            400
    );

    private static final String CATEGORY = "audit.http.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    HttpAuditValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
