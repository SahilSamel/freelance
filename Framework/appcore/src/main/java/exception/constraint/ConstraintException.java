package exception.constraint;

import error.IBadRequestError; // Change to your sub-interface
import exception.exception.Exception;

public class ConstraintException extends Exception {

    // Now it strictly demands an IConstraintError!
    public ConstraintException(IBadRequestError error) {
        super(error);
    }
}