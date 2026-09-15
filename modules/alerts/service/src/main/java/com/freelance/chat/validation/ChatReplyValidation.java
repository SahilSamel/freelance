package com.freelance.chat.validation;

import com.freelance.alerts.dto.AdminChatReplyRequestDTO;
import com.freelance.keys.alerts.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class ChatReplyValidation extends AbstractValidation {

    public void validateRequest(AdminChatReplyRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_USER_IDS, "userId");
            raiseValidationError(ValidationErrors.INVALID_CHAT_REPLY, "message");
            validate();
            return;
        }

        if (request.userId() == null) {
            raiseValidationError(ValidationErrors.INVALID_USER_IDS, "userId");
        }

        if (request.channel() == null) {
            raiseValidationError(ValidationErrors.INVALID_CHANNEL, "channel");
        }

        if (request.message() == null || request.message().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_CHAT_REPLY, "message");
        }

        validate();
    }
}