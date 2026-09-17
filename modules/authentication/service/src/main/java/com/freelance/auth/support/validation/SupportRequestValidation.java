package com.freelance.auth.support.validation;

import com.freelance.auth.dto.SupportRequestDTO;
import com.freelance.keys.authentication.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class SupportRequestValidation extends AbstractValidation {

    public void validateRequest(SupportRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_SUPPORT_REQUEST, "request");
            validate();
            return;
        }

        if (request.reason() == null || request.reason().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_SUPPORT_REASON, "reason");
        }

        if (request.ticketId() == null || request.ticketId().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_SUPPORT_TICKET, "ticketId");
        }

        validate();
    }
}
