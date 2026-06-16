package errorHandling;

import errorHandling.exception.validation.ValidationException;
import transaction.Transaction;
import error.ErrorCode;
import java.util.List;
import java.util.Map;

public abstract class AbstractValidation {
    /**
     * Registers a system or infrastructure failure against the current thread's transaction context.
     * This flags the context as dirty, guaranteeing the Launcher will roll back the database.
     * * @param validationCode The technical error key (e.g., "SMS_PROVIDER_DOWN") sent back to the UI.
     */
    protected void raiseValidationError(String validationCode, String fieldName) {
        if (validationCode != null && !validationCode.isBlank()) {
            Transaction.peek().addValidationError(fieldName, validationCode);
        }
    }
    protected void validate(){
        Map<String, List<String>> validationErrors = Transaction.peek().getValidationErrors();
        if(!validationErrors.isEmpty()){
            throw new ValidationException(ErrorCode.VALIDATION_ERROR.name());
        }
    }
}
