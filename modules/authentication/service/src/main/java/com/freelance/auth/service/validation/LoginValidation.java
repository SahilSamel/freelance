package com.freelance.auth.service.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.error.authentication.ValidationErrors;
import com.freelance.identityManagement.IdentityManagementKeys;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class LoginValidation extends AbstractValidation {

    public void validateRequest(AuthLoginRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_USERNAME, "request");
            validate();
            return;
        }

        int minUsernameLength = Integer.parseInt(ConfigurationEngine.get(IdentityManagementKeys.USERNAME_MINIMUM_LENGTH.getKey(), "3"));
        int maxUsernameLength = Integer.parseInt(ConfigurationEngine.get(IdentityManagementKeys.USERNAME_MAXIMUM_LENGTH.getKey(), "50"));

        if (request.username() == null || request.username().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_USERNAME, "username");
        } else if (request.username().length() < minUsernameLength || request.username().length() > maxUsernameLength) {
            raiseValidationError(ValidationErrors.INVALID_USERNAME, "username");
        }
        if (request.password() == null || request.password().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_PASSWORD, "password");
        }

        validate();
    }
}
