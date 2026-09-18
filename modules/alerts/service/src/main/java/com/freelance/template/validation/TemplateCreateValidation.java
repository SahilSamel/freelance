package com.freelance.template.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateCreateRequestDTO;
import com.freelance.keys.alerts.message.AlertEmitConfigKeys;
import com.freelance.keys.alerts.template.TemplateValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

import java.util.*;

public class TemplateCreateValidation extends AbstractValidation {

    public TemplateCreateValidation() {}

    public void validateRequest(TemplateCreateRequestDTO request) {
        if (request == null) {
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_KEY, "templateKey");
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_CONTENT, "content");
            validate();
            return;
        }

        List<TemplateCreateRequestDTO> allTemplates = request.allTemplates();
        if (allTemplates == null || allTemplates.isEmpty()) {
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_KEY, "templateKey");
            raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_CONTENT, "content");
            validate();
            return;
        }

        Set<String> seenKeys = new HashSet<>();
        Set<String> seenPayloads = new HashSet<>();

        for (TemplateCreateRequestDTO tpl : allTemplates) {
            if (tpl.templateKey() == null || tpl.templateKey().isBlank()) {
                raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_KEY, "templateKey");
            } else if (!seenKeys.add(tpl.templateKey())) {
                raiseValidationError(TemplateValidationErrors.DUPLICATE_TEMPLATE_KEY, "templateKey");
            }

            if (tpl.content() == null || tpl.content().isBlank()) {
                raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_CONTENT, "content");
            }

            if (tpl.priority() != null && !tpl.priority().isBlank()) {
                String p = tpl.priority().trim().toUpperCase();
                if (!p.equals("HIGH") && !p.equals("NORMAL") && !p.equals("LOW")) {
                    raiseValidationError(TemplateValidationErrors.INVALID_TEMPLATE_PRIORITY, "priority");
                }
            }

            AlertChannel channel = tpl.channel() != null
                    ? tpl.channel()
                    : ConfigurationEngine.getEnum(AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);

            if (tpl.buttons() != null && !tpl.buttons().isEmpty()) {
                if (channel != AlertChannel.WHATSAPP) {
                    raiseValidationError(TemplateValidationErrors.BUTTONS_NOT_SUPPORTED_FOR_CHANNEL, "buttons");
                } else {
                    if (tpl.buttons().size() > 10) {
                        raiseValidationError(TemplateValidationErrors.TOO_MANY_BUTTONS, "buttons");
                    }

                    Set<String> seenTexts = new HashSet<>();
                    Set<Integer> seenSequences = new HashSet<>();

                    for (TemplateButtonDTO b : tpl.buttons()) {
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
            }
        }

        validate();
    }
}