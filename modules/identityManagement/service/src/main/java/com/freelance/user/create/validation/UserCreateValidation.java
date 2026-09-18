package com.freelance.user.create.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.keys.identityManagement.user.UserConfigKeys;
import com.freelance.keys.identityManagement.user.UserValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.create.UserCreateRequestDTO;

public class UserCreateValidation extends AbstractValidation {

    public void validateRequest(UserCreateRequestDTO request) {
        if (request == null) {
            raiseValidationError(UserValidationErrors.INVALID_USERNAME, "username");
            raiseValidationError(UserValidationErrors.INVALID_PASSWORD, "password");
            validate();
            return;
        }

        int minUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MINIMUM_LENGTH);
        int maxUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MAXIMUM_LENGTH);
        int minPasswordLength = ConfigurationEngine.getInt(UserConfigKeys.PASSWORD_MINIMUM_LENGTH);

        String usernamePattern = ConfigurationEngine.get(UserConfigKeys.USERNAME_PATTERN);
        if (request.username() == null || request.username().isBlank()) {
            raiseValidationError(UserValidationErrors.INVALID_USERNAME, "username");
        } else {
            if (request.username().length() < minUsernameLength) {
                raiseValidationError(UserValidationErrors.USERNAME_TOO_SHORT, "username");
            }
            if (request.username().length() > maxUsernameLength) {
                raiseValidationError(UserValidationErrors.USERNAME_TOO_LONG, "username");
            }
            if (usernamePattern != null && !request.username().matches(usernamePattern)) {
                raiseValidationError(UserValidationErrors.USERNAME_HAS_INVALID_CHARS, "username");
            }
        }

        if (request.password() == null || request.password().isBlank()) {
            raiseValidationError(UserValidationErrors.INVALID_PASSWORD, "password");
        } else if (request.password().length() < minPasswordLength) {
            raiseValidationError(UserValidationErrors.PASSWORD_TOO_SHORT, "password");
        }

        validate();
    }
}
