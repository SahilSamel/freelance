package com.freelance.user.delete.validation;

import com.freelance.keys.identityManagement.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.delete.UserDeleteRequestDTO;

public class UserDeleteValidation extends AbstractValidation {

    public void validateRequest(UserDeleteRequestDTO request) {
        if (request == null || request.id() == null) {
            raiseValidationError(ValidationErrors.INVALID_USER_ID, "id");
        }

        validate();
    }
}
