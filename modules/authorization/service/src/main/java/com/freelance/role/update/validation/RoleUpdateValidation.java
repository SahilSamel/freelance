package com.freelance.role.update.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.keys.authorization.role.RoleConfigKeys;
import com.freelance.keys.authorization.role.RoleValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class RoleUpdateValidation extends AbstractValidation {

    public void validateRequest(RoleUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(RoleValidationErrors.INVALID_ROLE_ID, "id");
            validate();
            return;
        }

        if (request.id() == null) {
            raiseValidationError(RoleValidationErrors.INVALID_ROLE_ID, "id");
        }

        int minNameLength = ConfigurationEngine.getInt(RoleConfigKeys.ROLE_NAME_MINIMUM_LENGTH);
        int maxNameLength = ConfigurationEngine.getInt(RoleConfigKeys.ROLE_NAME_MAXIMUM_LENGTH);
        String namePattern = ConfigurationEngine.get(RoleConfigKeys.ROLE_NAME_PATTERN);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                raiseValidationError(RoleValidationErrors.INVALID_ROLE_NAME, "name");
            } else {
                if (request.name().length() < minNameLength) {
                    raiseValidationError(RoleValidationErrors.ROLE_NAME_TOO_SHORT, "name");
                }
                if (request.name().length() > maxNameLength) {
                    raiseValidationError(RoleValidationErrors.ROLE_NAME_TOO_LONG, "name");
                }
                if (namePattern != null && !request.name().matches(namePattern)) {
                    raiseValidationError(RoleValidationErrors.ROLE_NAME_HAS_INVALID_CHARS, "name");
                }
            }
        }

        validate();
    }
}
