package com.freelance.chat.validation;

import com.freelance.alerts.dto.ChatTimelineRequestDTO;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class ChatTimelineValidation extends AbstractValidation {

    public void validateRequest(ChatTimelineRequestDTO request) {
        if (request == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userId");
            raiseValidationError(AlertEmitValidationErrors.INVALID_CHANNEL, "channel");
            validate();
            return;
        }

        if (request.userId() == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userId");
        }

        if (request.channel() == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_CHANNEL, "channel");
        }

        validate();
    }
}
