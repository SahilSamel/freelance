package errorHandling.exception.validation;
import errorHandling.exception.BaseException;

public class ValidationException extends BaseException {
    public ValidationException(String validationCode) {
        super(validationCode);
    }
    public ValidationException(String validationCode, String diagnosticMessage) {

        super(validationCode, diagnosticMessage);
    }
}
