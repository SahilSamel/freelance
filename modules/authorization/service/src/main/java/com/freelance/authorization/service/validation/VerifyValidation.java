package com.freelance.authorization.service.validation;

import com.freelance.error.authorization.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.rest.app.transaction.Transaction;

public class VerifyValidation extends AbstractValidation {
    public void validateRequest(){
        String authKey = Transaction.getRequestContext().authorizationKey();
        if (authKey == null || authKey.isEmpty()) {
            raiseValidationError(ValidationErrors.UNAUTHORIZED, "security");
        }
        validate();
    }
}
