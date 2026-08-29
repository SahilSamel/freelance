package com.freelance.user.read.validation;

import com.freelance.keys.identityManagement.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.read.UserReadRequestDTO;

public class UserReadValidation extends AbstractValidation {

    public void validateRequest(UserReadRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_USER_ID, "id");
            validate();
            return;
        }

        if (request.id() == null && (request.username() == null || request.username().isBlank())) {
            raiseValidationError(ValidationErrors.INVALID_USER_ID, "id");
        }

        validate();
    }
}
