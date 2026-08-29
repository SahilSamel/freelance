package com.freelance.role.create.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.keys.authorization.AuthorizationConfigKeys;
import com.freelance.keys.authorization.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class RoleCreateValidation extends AbstractValidation {

    public void validateRequest(RoleCreateRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_ROLE_NAME, "name");
            validate();
            return;
        }

        int minNameLength = ConfigurationEngine.getInt(AuthorizationConfigKeys.ROLE_NAME_MINIMUM_LENGTH);
        int maxNameLength = ConfigurationEngine.getInt(AuthorizationConfigKeys.ROLE_NAME_MAXIMUM_LENGTH);
        String namePattern = ConfigurationEngine.get(AuthorizationConfigKeys.ROLE_NAME_PATTERN);

        if (request.name() == null || request.name().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_ROLE_NAME, "name");
        } else {
            if (request.name().length() < minNameLength) {
                raiseValidationError(ValidationErrors.ROLE_NAME_TOO_SHORT, "name");
            }
            if (request.name().length() > maxNameLength) {
                raiseValidationError(ValidationErrors.ROLE_NAME_TOO_LONG, "name");
            }
            if (namePattern != null && !request.name().matches(namePattern)) {
                raiseValidationError(ValidationErrors.ROLE_NAME_HAS_INVALID_CHARS, "name");
            }
        }

        validate();
    }
}
