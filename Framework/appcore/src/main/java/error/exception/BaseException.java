package error.exception;

public abstract class BaseException extends RuntimeException {
    private final String errorCode;

    protected BaseException(String errorCode) {
        super("Failure code triggered: " + errorCode);
        this.errorCode = errorCode;
    }

    // Constructor with a diagnostic message for internal logging
    protected BaseException(String errorCode, String diagnosticMessage) {
        super(diagnosticMessage);
        this.errorCode = errorCode;
    }

    // Full constructor allowing a root cause (ideal for System failures)
    protected BaseException(String errorCode, String diagnosticMessage, Throwable cause) {
        super(diagnosticMessage, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
