package com.freelance.role.delete.validation;

import com.freelance.authorization.dto.role.delete.RoleDeleteRequestDTO;
import com.freelance.keys.authorization.role.RoleValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class RoleDeleteValidation extends AbstractValidation {

    public void validateRequest(RoleDeleteRequestDTO request) {
        if (request == null || request.id() == null) {
            raiseValidationError(RoleValidationErrors.INVALID_ROLE_ID, "id");
        }
        validate();
    }
}
