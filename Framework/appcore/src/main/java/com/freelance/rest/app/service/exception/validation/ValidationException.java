package com.freelance.rest.app.service.exception.validation;

import com.freelance.keys.FrameworkErrorConstants;
import com.freelance.rest.app.service.exception.BaseException;

/**
 * Thrown when one or more accumulated field validation errors are found
 * for a request. Carries the generic {@link FrameworkErrorConstants#VALIDATION_ERROR}
 * as its top-level keys; the specific field errors are held separately
 * (e.g. in the transaction context) and surfaced by the exception handler.
 */
public class ValidationException extends BaseException {

    public ValidationException() {
        super(FrameworkErrorConstants.VALIDATION_ERROR);
    }
}