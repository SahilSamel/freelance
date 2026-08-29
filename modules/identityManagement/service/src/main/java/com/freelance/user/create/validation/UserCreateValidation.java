package com.freelance.user.create.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.keys.identityManagement.IdentityManagementKeys;
import com.freelance.keys.identityManagement.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.create.UserCreateRequestDTO;

public class UserCreateValidation extends AbstractValidation {

    public void validateRequest(UserCreateRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_USERNAME, "username");
            raiseValidationError(ValidationErrors.INVALID_PASSWORD, "password");
            validate();
            return;
        }

        int minUsernameLength = ConfigurationEngine.getInt(IdentityManagementKeys.USERNAME_MINIMUM_LENGTH);
        int maxUsernameLength = ConfigurationEngine.getInt(IdentityManagementKeys.USERNAME_MAXIMUM_LENGTH);
        int minPasswordLength = ConfigurationEngine.getInt(IdentityManagementKeys.PASSWORD_MINIMUM_LENGTH);

        String usernamePattern = ConfigurationEngine.get(IdentityManagementKeys.USERNAME_PATTERN);
        if (request.username() == null || request.username().isBlank()) {
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

        if (request.password() == null || request.password().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_PASSWORD, "password");
        } else if (request.password().length() < minPasswordLength) {
            raiseValidationError(ValidationErrors.PASSWORD_TOO_SHORT, "password");
        }

        validate();
    }
}
