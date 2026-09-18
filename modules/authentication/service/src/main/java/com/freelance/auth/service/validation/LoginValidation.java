package com.freelance.auth.service.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.keys.authentication.auth.AuthValidationErrors;
import com.freelance.keys.identityManagement.user.UserConfigKeys;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class LoginValidation extends AbstractValidation {

    public void validateRequest(AuthLoginRequestDTO request) {
        if (request == null) {
            raiseValidationError(AuthValidationErrors.INVALID_USERNAME, "request");
            validate();
            return;
        }

        int minUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MINIMUM_LENGTH);
        int maxUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MAXIMUM_LENGTH);

        if (request.username() == null || request.username().isBlank()) {
            raiseValidationError(AuthValidationErrors.INVALID_USERNAME, "username");
        } else if (request.username().length() < minUsernameLength || request.username().length() > maxUsernameLength) {
            raiseValidationError(AuthValidationErrors.INVALID_USERNAME, "username");
        }
        if (request.password() == null || request.password().isBlank()) {
            raiseValidationError(AuthValidationErrors.INVALID_PASSWORD, "password");
        }

        validate();
    }
}
