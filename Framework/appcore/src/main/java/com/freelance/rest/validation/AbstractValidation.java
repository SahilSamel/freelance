package com.freelance.rest.validation;

import com.freelance.error.IBadRequestError;
import com.freelance.exception.validation.ValidationException;
import com.freelance.transaction.Transaction;

import java.util.List;
import java.util.Map;

public abstract class AbstractValidation {

    /**
     * Accumulates an input format failure against a specific request field.
     * Uses type-safe IError to preserve full category pathing context.
     */
    protected void raiseValidationError(IBadRequestError validationError, String fieldName) {
        if (validationError != null && fieldName != null && !fieldName.isBlank()) {
            Transaction.peek().addValidationError(fieldName, validationError);
        }
    }

    /**
     * Evaluates all accumulated field validations. If any errors were registered during
     * the block execution, it fires a validation cycle termination exception.
     */
    protected void validate() {
        Map<String, List<IBadRequestError>> validationErrors = Transaction.peek().getValidationErrors();

        if (validationErrors != null && !validationErrors.isEmpty()) {
            throw new ValidationException();
        }
    }
}