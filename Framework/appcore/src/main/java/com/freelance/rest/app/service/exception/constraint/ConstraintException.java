package com.freelance.rest.app.service.exception.constraint;

import com.freelance.config.IError;
import com.freelance.rest.app.service.exception.BaseException;

/**
 * Thrown when an application rule constraint is violated.
 * <p>
 * Carries the specific {@link IError} describing the constraint violation.
 * </p>
 */
public class ConstraintException extends BaseException {

    public ConstraintException(IError error) {
        super(error);
    }
}