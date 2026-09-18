package com.freelance.user.update.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.keys.identityManagement.user.UserConfigKeys;
import com.freelance.keys.identityManagement.user.UserValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.update.UserUpdateRequestDTO;

public class UserUpdateValidation extends AbstractValidation {

    public void validateRequest(UserUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(UserValidationErrors.INVALID_USER_ID, "id");
            validate();
            return;
        }

        if (request.id() == null) {
            raiseValidationError(UserValidationErrors.INVALID_USER_ID, "id");
        }

        int minUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MINIMUM_LENGTH);
        int maxUsernameLength = ConfigurationEngine.getInt(UserConfigKeys.USERNAME_MAXIMUM_LENGTH);
        int minPasswordLength = ConfigurationEngine.getInt(UserConfigKeys.PASSWORD_MINIMUM_LENGTH);
        String usernamePattern = ConfigurationEngine.get(UserConfigKeys.USERNAME_PATTERN);

        if (request.username() != null) {
            if (request.username().isBlank()) {
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
        }

        if (request.password() != null) {
            if (request.password().isBlank()) {
                raiseValidationError(UserValidationErrors.INVALID_PASSWORD, "password");
            } else if (request.password().length() < minPasswordLength) {
                raiseValidationError(UserValidationErrors.PASSWORD_TOO_SHORT, "password");
            }
        }

        validate();
    }
}
