package exception.validation;

import error.FrameworkErrorConstants;
import exception.exception.Exception;

public class ValidationException extends Exception {

    public ValidationException() {
        super(FrameworkErrorConstants.VALIDATION_ERROR);
    }
}