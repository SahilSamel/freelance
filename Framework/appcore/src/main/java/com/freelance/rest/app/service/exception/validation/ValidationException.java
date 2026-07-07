package com.freelance.rest.app.service.exception.validation;

import com.freelance.error.FrameworkErrorConstants;
import com.freelance.rest.app.service.exception.BaseException;

public class ValidationException extends BaseException {

    public ValidationException() {
        super(FrameworkErrorConstants.VALIDATION_ERROR);
    }
}