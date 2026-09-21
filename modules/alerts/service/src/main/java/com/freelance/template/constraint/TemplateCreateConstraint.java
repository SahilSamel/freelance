package com.freelance.template.constraint;

import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateCreateRequestDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.keys.alerts.message.AlertEmitConfigKeys;
import com.freelance.keys.alerts.template.TemplateConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.*;

public class TemplateCreateConstraint extends AbstractConstraint {

    private final AlertTemplateRepository templateRepository;
    private final AlertButtonRepository buttonRepository;
    private final AlertTemplateCategoryRepository categoryRepository;

    public TemplateCreateConstraint() {
        this(new AlertTemplateRepository(), new AlertButtonRepository(), new AlertTemplateCategoryRepository());
    }

    public TemplateCreateConstraint(AlertTemplateRepository templateRepository) {
        this(templateRepository, new AlertButtonRepository(), new AlertTemplateCategoryRepository());
    }

    public TemplateCreateConstraint(AlertTemplateRepository templateRepository, AlertButtonRepository buttonRepository) {
        this(templateRepository, buttonRepository, new AlertTemplateCategoryRepository());
    }

    public TemplateCreateConstraint(AlertTemplateRepository templateRepository,
                                    AlertButtonRepository buttonRepository,
                                    AlertTemplateCategoryRepository categoryRepository) {
        this.templateRepository = templateRepository;
        this.buttonRepository = buttonRepository;
        this.categoryRepository = categoryRepository;
    }

    public void checkConstraints(TemplateCreateRequestDTO request) {
        if (request == null) return;

        Map<String, TemplateCreateRequestDTO> templateMap = new HashMap<>();
        for (TemplateCreateRequestDTO tpl : request.allTemplates()) {
            if (tpl.templateKey() != null && !tpl.templateKey().isBlank()) {
                templateMap.put(tpl.templateKey(), tpl);
            }
        }

        for (TemplateCreateRequestDTO tpl : request.allTemplates()) {
            if (tpl.templateKey() != null && isTemplateKeyInRepository(tpl.templateKey())) {
                raiseConstraintError(TemplateConstraintErrors.TEMPLATE_KEY_ALREADY_EXISTS);
            }

            AlertChannel channel = tpl.channel() != null
                    ? tpl.channel()
                    : ConfigurationEngine.getEnum(AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);

            if (channel == AlertChannel.WHATSAPP && tpl.category() != null && !tpl.category().isBlank()) {
                if (!isCategoryInRepository(tpl.category())) {
                    raiseConstraintError(TemplateConstraintErrors.TEMPLATE_CATEGORY_NOT_FOUND);
                }
            }

            if (tpl.buttons() != null) {
                for (TemplateButtonDTO btn : tpl.buttons()) {
                    if (btn.type() == ButtonType.QUICK_REPLY && btn.payload() != null && !btn.payload().isBlank()) {
                        if (isButtonPayloadInRepository(btn.payload())) {
                            raiseConstraintError(TemplateConstraintErrors.BUTTON_PAYLOAD_ALREADY_EXISTS);
                        }
                    }
                }
            }
        }

        for (TemplateCreateRequestDTO tpl : request.allTemplates()) {
            AlertChannel channel = tpl.channel() != null
                    ? tpl.channel()
                    : ConfigurationEngine.getEnum(AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);

            if (channel == AlertChannel.WHATSAPP && tpl.buttons() != null && !tpl.buttons().isEmpty()) {
                if (tpl.templateKey() != null && !tpl.templateKey().isBlank()) {
                    validateChainFrom(tpl, templateMap, new ArrayList<>(List.of(tpl.templateKey())));
                }
            }
        }
    }

    private void validateChainFrom(
            TemplateCreateRequestDTO current,
            Map<String, TemplateCreateRequestDTO> templateMap,
            List<String> currentPath
    ) {
        if (current == null || current.buttons() == null || current.buttons().isEmpty()) {
            return;
        }

        for (TemplateButtonDTO btn : current.buttons()) {
            String replyKey = btn.replyTemplateKey();

            boolean isTerminalAction = btn.requiresHuman()
                    || btn.type() == ButtonType.URL
                    || btn.type() == ButtonType.PHONE_NUMBER;

            if (replyKey == null || replyKey.isBlank()) {
                if (!isTerminalAction) {
                    raiseConstraintError(TemplateConstraintErrors.TEMPLATE_CHAIN_BROKEN);
                }
                continue;
            }

            if (currentPath.contains(replyKey)) {
                // Loop detected on this path: valid termination
                continue;
            }

            if (templateMap.containsKey(replyKey)) {
                TemplateCreateRequestDTO nextTemplate = templateMap.get(replyKey);
                List<String> nextPath = new ArrayList<>(currentPath);
                nextPath.add(replyKey);
                validateChainFrom(nextTemplate, templateMap, nextPath);
            } else {
                Optional<AlertTemplateEntity> existingOpt = findTemplateInRepository(replyKey);
                if (existingOpt.isEmpty()) {
                    raiseConstraintError(TemplateConstraintErrors.TEMPLATE_CHAIN_BROKEN);
                }

                AlertTemplateEntity existing = existingOpt.get();
                List<String> nextPath = new ArrayList<>(currentPath);
                nextPath.add(replyKey);
                validateChainFromEntity(existing, templateMap, nextPath);
            }
        }
    }

    private void validateChainFromEntity(
            AlertTemplateEntity current,
            Map<String, TemplateCreateRequestDTO> templateMap,
            List<String> currentPath
    ) {
        if (current == null || current.getButtons() == null || current.getButtons().isEmpty()) {
            return;
        }

        for (AlertButtonEntity btn : current.getButtons()) {
            boolean isTerminalAction = (btn.getReply() != null && btn.getReply().isRequiresHumanAgent())
                    || btn.getButtonType() == ButtonType.URL
                    || btn.getButtonType() == ButtonType.PHONE_NUMBER;

            if (isTerminalAction) {
                continue;
            }

            AlertTemplateEntity replyTemplate = (btn.getReply() != null) ? btn.getReply().getReplyTemplate() : null;
            String replyKey = (replyTemplate != null) ? replyTemplate.getTemplateKey() : null;

            if (replyKey == null || replyKey.isBlank()) {
                continue;
            }

            if (currentPath.contains(replyKey)) {
                continue;
            }

            List<String> nextPath = new ArrayList<>(currentPath);
            nextPath.add(replyKey);

            if (templateMap.containsKey(replyKey)) {
                validateChainFrom(templateMap.get(replyKey), templateMap, nextPath);
            } else {
                validateChainFromEntity(replyTemplate, templateMap, nextPath);
            }
        }
    }

    private boolean isTemplateKeyInRepository(String templateKey) {
        if (templateRepository == null || templateKey == null || templateKey.isBlank()) {
            return false;
        }
        try {
            return templateRepository.findByKey(templateKey.trim()).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isButtonPayloadInRepository(String payload) {
        if (buttonRepository == null || payload == null || payload.isBlank()) {
            return false;
        }
        try {
            return buttonRepository.findByPayload(payload.trim()).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCategoryInRepository(String categoryKey) {
        if (categoryRepository == null || categoryKey == null || categoryKey.isBlank()) {
            return false;
        }
        try {
            return categoryRepository.findByKey(categoryKey.trim()).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<AlertTemplateEntity> findTemplateInRepository(String templateKey) {
        if (templateRepository == null || templateKey == null || templateKey.isBlank()) {
            return Optional.empty();
        }
        try {
            return templateRepository.findByKey(templateKey.trim());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}