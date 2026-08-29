package com.freelance.user.update.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.keys.identityManagement.IdentityManagementKeys;
import com.freelance.keys.identityManagement.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.update.UserUpdateRequestDTO;

public class UserUpdateValidation extends AbstractValidation {

    public void validateRequest(UserUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_USER_ID, "id");
            validate();
            return;
        }

        if (request.id() == null) {
            raiseValidationError(ValidationErrors.INVALID_USER_ID, "id");
        }

        int minUsernameLength = ConfigurationEngine.getInt(IdentityManagementKeys.USERNAME_MINIMUM_LENGTH);
        int maxUsernameLength = ConfigurationEngine.getInt(IdentityManagementKeys.USERNAME_MAXIMUM_LENGTH);
        int minPasswordLength = ConfigurationEngine.getInt(IdentityManagementKeys.PASSWORD_MINIMUM_LENGTH);
        String usernamePattern = ConfigurationEngine.get(IdentityManagementKeys.USERNAME_PATTERN);

        if (request.username() != null) {
            if (request.username().isBlank()) {
                raiseValidationError(ValidationErrors.INVALID_USERNAME, "username");
            } else {
                if (request.username().length() < minUsernameLength) {
                    raiseValidationError(ValidationErrors.USERNAME_TOO_SHORT, "username");
                }
                if (request.username().length() > maxUsernameLength) {
                    raiseValidationError(ValidationErrors.USERNAME_TOO_LONG, "username");
                }
                if (usernamePattern != null && !request.username().matches(usernamePattern)) {
                    raiseValidationError(ValidationErrors.USERNAME_HAS_INVALID_CHARS, "username");
                }
            }
        }

        if (request.password() != null) {
            if (request.password().isBlank()) {
                raiseValidationError(ValidationErrors.INVALID_PASSWORD, "password");
            } else if (request.password().length() < minPasswordLength) {
                raiseValidationError(ValidationErrors.PASSWORD_TOO_SHORT, "password");
            }
        }

        validate();
    }
}
