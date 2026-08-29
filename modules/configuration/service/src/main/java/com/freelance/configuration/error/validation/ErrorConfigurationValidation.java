package com.freelance.configuration.error.validation;

import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateRequestDTO;
import com.freelance.keys.configuration.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class ErrorConfigurationValidation extends AbstractValidation {

    public void validateUpdateRequest(ErrorConfigurationUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_ERROR_ID, "errorId");
            validate();
            return;
        }

        if (request.errorId() == null || request.errorId().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_ERROR_ID, "errorId");
        }

        if (request.statusCode() != null) {
            if (request.statusCode() < 100 || request.statusCode() > 599) {
                raiseValidationError(ValidationErrors.INVALID_STATUS_CODE, "statusCode");
            }
        }

        if (request.errorMessage() != null && request.errorMessage().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_ERROR_MESSAGE, "errorMessage");
        }

        validate();
    }
}
