package errorHandling.exception.constraint;

import errorHandling.exception.BaseException;

public class ConstraintException extends BaseException {
    public ConstraintException(String errorCode) {
        super(errorCode);
    }
    public ConstraintException(String errorCode, String diagnosticMessage) {
        super(errorCode, diagnosticMessage);
    }
}
