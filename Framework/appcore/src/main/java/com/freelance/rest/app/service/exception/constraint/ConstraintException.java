package com.freelance.rest.app.service.exception.constraint;

import com.freelance.error.IBadRequestError; // Change to your sub-interface
import com.freelance.rest.app.service.exception.BaseException;

public class ConstraintException extends BaseException {
    public ConstraintException(IBadRequestError error) {
        super(error);
    }
}