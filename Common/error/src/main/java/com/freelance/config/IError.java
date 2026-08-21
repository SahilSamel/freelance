package com.freelance.config;

/**
 * Base contract for application errors, providing a unique error key,
 * unique indirection code (e.g. FL_1001), default HTTP status code,
 * default user-facing message, and description.
 */
public interface IError {

    /**
     * Fully-qualified unique key used for database lookup and configuration (e.g. "authentication.validation.INVALID_USERNAME").
     */
    String getErrorKey();

    /**
     * Unique, non-descriptive error code returned in client responses (e.g. "FL_1001").
     */
    String getIndirectionCode();

    /**
     * Default HTTP status code associated with this error (e.g. 400, 401, 404, 409, 500).
     */
    int getDefaultStatusCode();

    /**
     * Default user-facing error message (e.g. "The provided username is invalid.").
     */
    String getDefaultMessage();

    /**
     * Human-readable description of what conditions trigger this error.
     */
    String getDescription();
}