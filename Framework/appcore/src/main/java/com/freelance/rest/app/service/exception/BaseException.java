package com.freelance.rest.app.service.exception;

import com.freelance.config.IError;

/**
 * Serves as the abstract base class for all application-specific runtime exceptions.
 * <p>
 * Binds an {@link IError} token to the runtime failure, allowing the JAX-RS
 * exception mapping layer to resolve error indirection codes, localized messages,
 * and HTTP response status codes.
 * </p>
 */
public abstract class BaseException extends RuntimeException {
    private final IError error;

    protected BaseException(IError error) {
        super("Failure triggered for error key: " + (error != null ? error.getErrorKey() : "unknown"));
        this.error = error;
    }

    protected BaseException(IError error, String diagnosticMessage) {
        super(diagnosticMessage);
        this.error = error;
    }

    protected BaseException(IError error, String diagnosticMessage, Throwable cause) {
        super(diagnosticMessage, cause);
        this.error = error;
    }

    public IError getError() {
        return error;
    }
}