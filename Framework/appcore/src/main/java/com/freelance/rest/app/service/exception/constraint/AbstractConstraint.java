package com.freelance.rest.app.service.exception.constraint;

import com.freelance.error.IBadRequestError;

public abstract class AbstractConstraint {

    /**
     * Raises a business rule constraint violation.
     * This immediately breaks the execution flow by throwing a ConstraintException,
     * signaling the ExceptionHandler to roll back the database and issue a response
     * driven by the specific constraint configuration (defaulting to 400).
     *
     * @param error The specific IConstraintError token (e.g., ConstraintErrors.USERNAME_ALREADY_TAKEN).
     */
    protected void raiseConstraintError(IBadRequestError error) {
        if (error != null) {
            throw new ConstraintException(error);
        }
    }
}