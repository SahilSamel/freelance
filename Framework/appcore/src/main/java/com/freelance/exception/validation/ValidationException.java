package com.freelance.exception.validation;

import com.freelance.error.FrameworkErrorConstants;
import com.freelance.exception.exception.Exception;

public class ValidationException extends Exception {

    public ValidationException() {
        super(FrameworkErrorConstants.VALIDATION_ERROR);
    }
}