package errorHandling;

import transaction.Transaction;

public abstract class AbstractValidation {
    /**
     * Registers a system or infrastructure failure against the current thread's transaction context.
     * This flags the context as dirty, guaranteeing the Launcher will roll back the database.
     * * @param validationCode The technical error key (e.g., "SMS_PROVIDER_DOWN") sent back to the UI.
     */
    protected void raiseConstraint(String validationCode) {
        if (validationCode != null && !validationCode.isBlank()) {
            Transaction.peek().addValidationError(validationCode);
        }
    }
}
