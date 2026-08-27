package com.freelance.rest.app.service.exception.validation;

import com.freelance.config.IError;
import com.freelance.rest.app.transaction.Transaction;

import java.util.List;
import java.util.Map;

/**
 * Base class for request input validation handlers.
 * <p>
 * Extending classes are expected to call {@link #raiseValidationError(IError, String)} across individual
 * field checks to accumulate failures without interrupting execution, and then call {@link #validate()}
 * at the end of the validation routine to evaluate all recorded errors and halt execution if any exist.
 * </p>
 */
public abstract class AbstractValidation {

    /**
     * Records a validation error for a specific field name, which will be included in the final error response payload.
     *
     * @param validationError the validation error descriptor to record; ignored if {@code null}
     * @param fieldName       the field name that will be present in the final error response; must not be null or blank
     */
    protected void raiseValidationError(IError validationError, String fieldName) {
        if (validationError != null && fieldName != null && !fieldName.isBlank()) {
            Transaction.peek().addValidationError(fieldName, validationError);
        }
    }

    /**
     * Evaluates all validation errors accumulated on the current transaction context.
     * <p>
     * If one or more validation errors have been registered, this method throws a {@link ValidationException}
     * to halt further request processing and return the aggregated validation error response.
     * </p>
     *
     * @throws ValidationException if any validation errors have been recorded
     */
    protected void validate() {
        Map<String, List<IError>> validationErrors = Transaction.peek().getValidationErrors();

        if (validationErrors != null && !validationErrors.isEmpty()) {
            throw new ValidationException();
        }
    }
}