package com.freelance.template.validation;

import com.freelance.alerts.ButtonType;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateUpdateRequestDTO;
import com.freelance.keys.alerts.template.TemplateValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

import java.util.HashSet;
import java.util.Set;

public class TemplateUpdateValidation extends AbstractValidation {

    public TemplateUpdateValidation() {}

    public void validateRequest(TemplateUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_ID, "id");
            validate();
            return;
        }

        if (request.id() == null && (request.templateKey() == null || request.templateKey().isBlank())) {
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_ID, "id");
        }

        if (request.content() != null && request.content().isBlank()) {
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_CONTENT, "content");
        }

        if (request.priority() != null && !request.priority().isBlank()) {
            String p = request.priority().trim().toUpperCase();
            if (!p.equals("HIGH") && !p.equals("NORMAL") && !p.equals("LOW")) {
                raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_PRIORITY, "priority");
            }
        }

        if (request.buttons() != null) {
            if (request.buttons().size() > 10) {
                raiseValidationError(TemplateValidationErrors.TOO_MANY_BUTTONS, "buttons");
            }

            Set<String> seenTexts = new HashSet<>();
            Set<Integer> seenSequences = new HashSet<>();
            Set<String> seenPayloads = new HashSet<>();

            for (TemplateButtonDTO b : request.buttons()) {
                if (b.text() == null || b.text().isBlank()) {
                    raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_BUTTON, "buttons.text");
                } else if (!seenTexts.add(b.text().trim().toLowerCase())) {
                    raiseValidationError(TemplateValidationErrors.DUPLICATE_BUTTON_TEXT, "buttons.text");
                }

                if (b.type() == null) {
                    raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_BUTTON, "buttons.type");
                }

                if (!seenSequences.add(b.sequenceOrder())) {
                    raiseValidationError(TemplateValidationErrors.DUPLICATE_BUTTON_SEQUENCE, "buttons.sequenceOrder");
                }

                if (b.type() == ButtonType.QUICK_REPLY && b.payload() != null && !b.payload().isBlank()) {
                    if (!seenPayloads.add(b.payload().trim())) {
                        raiseValidationError(TemplateValidationErrors.DUPLICATE_BUTTON_PAYLOAD, "buttons.payload");
                    }
                }

                boolean isTerminalAction = b.requiresHuman()
                        || b.type() == ButtonType.URL
                        || b.type() == ButtonType.PHONE_NUMBER;

                if ((b.replyTemplateKey() == null || b.replyTemplateKey().isBlank()) && !isTerminalAction) {
                    raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_BUTTON, "buttons.replyTemplateKey");
                }
            }
        }

        validate();
    }
}
