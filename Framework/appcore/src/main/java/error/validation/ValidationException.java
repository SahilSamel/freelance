package error.validation;

import error.exception.BaseException;

public class ValidationException extends BaseException {
    public ValidationException(String validationCode) {
        super(validationCode);
    }
    public ValidationException(String validationCode, String diagnosticMessage) {
        super(validationCode, diagnosticMessage);
    }
}
