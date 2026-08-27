package com.freelance.rest.app.service.exception.constraint;

import com.freelance.config.IError;

/**
 * Base class to be extended by domain constraint checkers to application constraints.
 */
public abstract class AbstractConstraint {

    /**
     * Raises a domain constraint violation and immediately stops code execution
     * right where it is called by throwing a {@link ConstraintException}.
     *
     * @param error the specific error descriptor describing the violated constraint; ignored if {@code null}
     * @throws ConstraintException immediately upon invocation with a non-null error, halting further execution
     */
    protected void raiseConstraintError(IError error) {
        if (error != null) {
            throw new ConstraintException(error);
        }
    }
}