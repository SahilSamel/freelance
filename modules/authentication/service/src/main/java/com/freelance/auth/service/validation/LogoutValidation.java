package com.freelance.auth.service.validation;

import com.freelance.auth.dto.AuthLogoutRequestDTO;
import com.freelance.keys.authentication.auth.AuthValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class LogoutValidation extends AbstractValidation {

    public void validateRequest(AuthLogoutRequestDTO request) {
        if (request == null || request.token() == null) {
            raiseValidationError(AuthValidationErrors.INVALID_TOKEN, "token");
        }
    }

}
