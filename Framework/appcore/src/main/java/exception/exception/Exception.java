package exception.exception;

import error.IError;

public abstract class Exception extends RuntimeException {
    private final IError error;

    protected Exception(IError error) {
        super("Failure code triggered: " + error.getCode());
        this.error = error;
    }

    // Constructor with a diagnostic message for internal logging
    protected Exception(IError error, String diagnosticMessage) {
        super(diagnosticMessage);
        this.error = error;
    }

    // Full constructor allowing a root cause (ideal for System failures)
    protected Exception(IError error, String diagnosticMessage, Throwable cause) {
        super(diagnosticMessage, cause);
        this.error = error;
    }

    public IError getError() {
        return error;
    }
}