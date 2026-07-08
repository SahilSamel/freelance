package com.freelance.error;

/**
 * Marker interface for errors that should default to an HTTP 400
 * (Bad Request) status unless explicitly overridden.
 */
public interface IBadRequestError extends IError {

    /**
     * Defaults to 400; implementors may override for a different status.
     */
    @Override
    default int getStatusCode() {
        return 400;
    }
}