package com.freelance.rest.app.service.exception.constraint;

import com.freelance.error.IBadRequestError; // Change to your sub-interface
import com.freelance.rest.app.service.exception.BaseException;

/**
 * Thrown when a business rule constraint is violated (e.g. duplicate
 * resource, conflicting state). Carries the specific {@link IBadRequestError}
 * describing the violation.
 */
public class ConstraintException extends BaseException {
    public ConstraintException(IBadRequestError error) {
        super(error);
    }
}