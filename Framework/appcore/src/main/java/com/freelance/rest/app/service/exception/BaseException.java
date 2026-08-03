package com.freelance.rest.app.service.exception;

import com.freelance.error.IError;

/**
 * Base class for all application-specific runtime exceptions. Carries
 * the structured {@link IError} that caused the failure, so it can be
 * mapped to a proper HTTP response by the exception handler.
 */
public abstract class BaseException extends RuntimeException {
    private final IError error;

    /**
     * Builds a default message from the error's code.
     *
     * @param error the structured error describing this failure
     */
    protected BaseException(IError error) {
        super("Failure code triggered: " + error.getCode());
        this.error = error;
    }

    /**
     * Constructor with a diagnostic message for internal logging.
     *
     * @param error             the structured error describing this failure
     * @param diagnosticMessage a human-readable message for logs/diagnostics
     */
    protected BaseException(IError error, String diagnosticMessage) {
        super(diagnosticMessage);
        this.error = error;
    }

    /**
     * Full constructor allowing a root cause.
     *
     * @param error             the structured error describing this failure
     * @param diagnosticMessage a human-readable message for logs/diagnostics
     * @param cause             the underlying exception that triggered this failure
     */
    protected BaseException(IError error, String diagnosticMessage, Throwable cause) {
        super(diagnosticMessage, cause);
        this.error = error;
    }

    public IError getError() {
        return error;
    }
}