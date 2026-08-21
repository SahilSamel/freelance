package com.freelance.auth.session.validation;

import com.freelance.auth.dto.SessionVerifyRequestDTO;
import com.freelance.keys.authentication.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class VerifySessionValidation extends AbstractValidation {

    public void validateRequest(SessionVerifyRequestDTO sessionVerifyRequestDTO) {
        if (sessionVerifyRequestDTO == null || sessionVerifyRequestDTO.sessionKey() == null || sessionVerifyRequestDTO.userId() == null) {
            raiseValidationError(ValidationErrors.INVALID_SESSION_KEY, "sessionKey");
        }

        validate();
    }
}
