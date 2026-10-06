package com.freelance.chat.validation;

import com.freelance.alerts.dto.AdminChatReplyRequestDTO;
import com.freelance.keys.alerts.chat.ChatValidationErrors;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class ChatReplyValidation extends AbstractValidation {

    public void validateRequest(AdminChatReplyRequestDTO request) {
        if (request == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userId");
            raiseValidationError(ChatValidationErrors.INVALID_CHAT_REPLY, "message");
            validate();
            return;
        }

        if (request.userId() == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_USER_IDS, "userId");
        }

        if (request.channel() == null) {
            raiseValidationError(AlertEmitValidationErrors.INVALID_CHANNEL, "channel");
        }

        boolean hasText = request.message() != null && !request.message().isBlank();
        boolean hasMedia = (request.mediaId() != null && !request.mediaId().isBlank())
                || (request.mediaUrl() != null && !request.mediaUrl().isBlank());

        if (!hasText && !hasMedia) {
            raiseValidationError(ChatValidationErrors.INVALID_CHAT_REPLY, "message");
        }

        validate();
    }
}