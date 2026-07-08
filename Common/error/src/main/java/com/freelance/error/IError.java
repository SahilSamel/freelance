package com.freelance.error;

/**
 * Base contract for application errors, providing a machine-readable
 * code, an HTTP status code, and a config key for message/lookup resolution.
 */
public interface IError {

    /**
     * Machine-readable, unique error code (e.g. "INVALID_EMAIL_FORMAT").
     */
    String getCode();

    /** HTTP status code associated with this error. */
    int getStatusCode();

    /** Fully-qualified config key used to look up messages/settings for this error. */
    String getConfigKey();
}