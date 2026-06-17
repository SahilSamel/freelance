package rest.validation;

import exception.validation.ValidationException;
import transaction.Transaction;

import java.util.List;
import java.util.Map;

public abstract class AbstractValidation {

    /**
     * Accumulates an input format failure against a specific request field.
     * These are buffered silently in the context so multiple form field errors can be collected at once.
     */
    protected void raiseValidationError(String validationCode, String fieldName) {
        if (validationCode != null && !validationCode.isBlank() && fieldName != null && !fieldName.isBlank()) {
            Transaction.peek().addValidationError(fieldName, validationCode);
        }
    }

    /**
     * Evaluates all accumulated field validations. If any errors were registered during
     * the block execution, it fires a validation cycle termination exception.
     */
    protected void validate() {
        Map<String, List<String>> validationErrors = Transaction.peek().getValidationErrors();

        if (validationErrors != null && !validationErrors.isEmpty()) {
            // Leverages the clean, no-arg default constructor we built earlier
            throw new ValidationException();
        }
    }
}