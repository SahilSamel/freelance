package com.freelance.auth.service.validation;

import com.freelance.auth.dto.AuthLogoutRequestDTO;
import com.freelance.error.authentication.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class LogoutValidation extends AbstractValidation {

    public void validateRequest(AuthLogoutRequestDTO request) {
        if (request == null || request.token() == null) {
            raiseValidationError(ValidationErrors.INVALID_TOKEN, "token");
        }
    }

}
