package com.freelance.rest.app.service.exception;

import com.freelance.error.IError;

public abstract class BaseException extends RuntimeException {
    private final IError error;

    protected BaseException(IError error) {
        super("Failure code triggered: " + error.getCode());
        this.error = error;
    }

    // Constructor with a diagnostic message for internal logging
    protected BaseException(IError error, String diagnosticMessage) {
        super(diagnosticMessage);
        this.error = error;
    }

    // Full constructor allowing a root cause (ideal for System failures)
    protected BaseException(IError error, String diagnosticMessage, Throwable cause) {
        super(diagnosticMessage, cause);
        this.error = error;
    }

    public IError getError() {
        return error;
    }
}