package com.freelance.alerts.validation;

import com.freelance.alerts.dto.AlertEmitRequestDTO;
import com.freelance.keys.alerts.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class AlertEmitValidation extends AbstractValidation {

    public void validateRequest(AlertEmitRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_TEMPLATE_ID, "templateId");
            raiseValidationError(ValidationErrors.INVALID_USER_IDS, "userIds");
            validate();
            return;
        }

        if (request.templateId() == null) {
            raiseValidationError(ValidationErrors.INVALID_TEMPLATE_ID, "templateId");
        }

        if (request.userIds() == null || request.userIds().isEmpty()) {
            raiseValidationError(ValidationErrors.INVALID_USER_IDS, "userIds");
        } else {
            for (int i = 0; i < request.userIds().size(); i++) {
                if (request.userIds().get(i) == null) {
                    raiseValidationError(ValidationErrors.INVALID_USER_IDS, "userIds[" + i + "]");
                }
            }
        }

        validate();
    }
}