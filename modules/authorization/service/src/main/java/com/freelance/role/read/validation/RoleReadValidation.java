package com.freelance.role.read.validation;

import com.freelance.authorization.dto.role.read.RoleReadRequestDTO;
import com.freelance.keys.authorization.role.RoleValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class RoleReadValidation extends AbstractValidation {

    public void validateRequest(RoleReadRequestDTO request) {
        if (request == null || (request.id() == null && (request.name() == null || request.name().isBlank()))) {
            raiseValidationError(RoleValidationErrors.INVALID_ROLE_ID, "id");
        }
        validate();
    }
}
