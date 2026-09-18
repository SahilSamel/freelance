package com.freelance.alerts.validation;

import com.freelance.alerts.dto.AlertEmitRequestDTO;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.keys.alerts.template.TemplateValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class AlertEmitValidation extends AbstractValidation {

    public void validateRequest(AlertEmitRequestDTO request) {
        if (request == null) {
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_ID, "templateId");
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userIds");
            validate();
            return;
        }

        if (request.templateId() == null) {
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_ID, "templateId");
        }

        if (request.userIds() == null || request.userIds().isEmpty()) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userIds");
        } else {
            for (int i = 0; i < request.userIds().size(); i++) {
                if (request.userIds().get(i) == null) {
                    raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userIds[" + i + "]");
                }
            }
        }

        validate();
    }
}