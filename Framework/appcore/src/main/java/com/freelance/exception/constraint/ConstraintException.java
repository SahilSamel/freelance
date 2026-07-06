package com.freelance.exception.constraint;

import com.freelance.error.IBadRequestError; // Change to your sub-interface
import com.freelance.exception.exception.Exception;

public class ConstraintException extends Exception {
    public ConstraintException(IBadRequestError error) {
        super(error);
    }
}