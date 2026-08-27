package com.freelance.rest.app.service.exception.validation;

import com.freelance.keys.FrameworkErrorConstants;
import com.freelance.rest.app.service.exception.BaseException;

/**
 * Thrown when one or more accumulated field validation errors are detected during request data processing.
 */
public class ValidationException extends BaseException {

    public ValidationException() {
        super(FrameworkErrorConstants.VALIDATION_ERROR);
    }
}