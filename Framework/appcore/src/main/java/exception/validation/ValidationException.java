package exception.validation;

import error.FrameworkErrorConstants;
import exception.exception.Exception;

public class ValidationException extends Exception {

    // SMART DEFAULT: Automatically binds to the core VALIDATION_ERROR constant
    public ValidationException() {
        super(FrameworkErrorConstants.VALIDATION_ERROR);
    }
}