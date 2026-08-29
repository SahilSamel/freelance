package com.freelance.keys.audit;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation error keys for the audit domain.
 * <p>
 * Implements {@link IError} to provide standardized 400 Bad Request error
 * indirection codes, user messages, and trigger condition descriptions when auditing
 * HTTP requests and responses.
 * </p>
 */
public enum ValidationErrors implements IError {

    /**
     * Raised when the HTTP method name is missing or blank.
     */
    MISSING_HTTP_METHOD(
            "FL_5001",
            "HTTP method is required.",
            "Missing or blank HTTP method",
            400
    ),

    /**
     * Raised when the endpoint path is missing or blank.
     */
    MISSING_ENDPOINT(
            "FL_5002",
            "Endpoint path is required.",
            "Missing or blank endpoint",
            400
    ),

    /**
     * Raised when the HTTP response status code is invalid or missing.
     */
    MISSING_STATUS_CODE(
            "FL_5003",
            "HTTP status code is required.",
            "Missing or invalid status code",
            400
    ),

    /**
     * Raised when the audit timestamp is null or unparseable.
     */
    MISSING_TIMESTAMP(
            "FL_5004",
            "Timestamp is required.",
            "Missing timestamp",
            400
    ),

    /**
     * Raised when the recorded execution duration is negative.
     */
    INVALID_DURATION(
            "FL_5005",
            "Duration must be greater than or equal to zero.",
            "Duration is negative or invalid",
            400
    ),

    /**
     * Raised when the distributed trace identifier is missing or blank.
     */
    MISSING_TRACE_ID(
            "FL_5006",
            "Trace ID is required.",
            "Missing or blank trace ID",
            400
    ),

    /**
     * Raised when the pagination limit is non-positive or invalid.
     */
    INVALID_LIMIT(
            "FL_5007",
            "Limit must be greater than zero.",
            "Pagination limit is non-positive or exceeds maximum allowed",
            400
    ),

    /**
     * Raised when the pagination offset is negative.
     */
    INVALID_OFFSET(
            "FL_5008",
            "Offset must be greater than or equal to zero.",
            "Pagination offset is negative",
            400
    ),

    /**
     * Raised when fromTimestamp is after toTimestamp.
     */
    INVALID_DATE_RANGE(
            "FL_5009",
            "Start timestamp cannot be after end timestamp.",
            "fromTimestamp is greater than toTimestamp",
            400
    );

    private static final String CATEGORY = "audit.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    /**
     * Constructs an audit validation error definition.
     *
     * @param indirectionCode   opaque client-facing error code (e.g., "FL_5001")
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
     * @return hierarchical key string (e.g., "audit.validation.MISSING_HTTP_METHOD")
     */
    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    /**
     * Retrieves the unique client-facing indirection error code.
     *
     * @return opaque indirection code string (e.g., "FL_5001")
     */
    @Override
    public String getIndirectionCode() {
        return this.indirectionCode;
    }

    /**
     * Retrieves the default localized error message.
     *
     * @return default message string
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
