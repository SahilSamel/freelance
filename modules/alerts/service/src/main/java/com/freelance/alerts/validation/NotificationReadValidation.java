package com.freelance.alerts.validation;

import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class NotificationReadValidation extends AbstractValidation {

    public void validateRequest(NotificationReadRequestDTO request) {
        if (request == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userId");
            validate();
            return;
        }

        if (request.userId() == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userId");
        }

        validate();
    }
}
