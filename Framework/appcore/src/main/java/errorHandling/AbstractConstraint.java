package errorHandling;

import transaction.Transaction;

public abstract class AbstractConstraint {
    /**
     * Registers a system or infrastructure failure against the current thread's transaction context.
     * This flags the context as dirty, guaranteeing the Launcher will roll back the database.
     * * @param constraintCode The technical error key (e.g., "SMS_PROVIDER_DOWN") sent back to the UI.
     */
    protected void raiseConstraint(String constraintCode) {
        if (constraintCode != null && !constraintCode.isBlank()) {
            Transaction.peek().addConstraintError(constraintCode);
        }
    }
}
