package com.freelance.auth.service.validation;

import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.keys.authentication.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class VerifyAuthValidation extends AbstractValidation {

    public void validateRequest(AuthVerifyRequestDTO authVerifyRequestDTO) {
        if (authVerifyRequestDTO == null || authVerifyRequestDTO.token() == null || authVerifyRequestDTO.token().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_TOKEN, "token");
        }
        validate();
    }
}
