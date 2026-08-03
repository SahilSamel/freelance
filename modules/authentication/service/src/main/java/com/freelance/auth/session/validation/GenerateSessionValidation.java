package com.freelance.auth.session.validation;

import com.freelance.auth.dto.SessionGenerateRequestDTO;
import com.freelance.error.authentication.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class GenerateSessionValidation extends AbstractValidation {

    public void validateRequest(SessionGenerateRequestDTO request) {
        if (request == null || request.userId() == null) {
            raiseValidationError(ValidationErrors.UNABLE_TO_CREATE_SESSION, "userId");
        }
        validate();
    }
}
