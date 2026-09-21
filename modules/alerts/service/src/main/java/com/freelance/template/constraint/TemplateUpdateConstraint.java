package com.freelance.template.constraint;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateUpdateRequestDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.templates.InboundWelcomeTemplate;
import com.freelance.keys.alerts.template.TemplateConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.Optional;

public class TemplateUpdateConstraint extends AbstractConstraint {

    private final AlertTemplateRepository templateRepository;
    private final AlertButtonRepository buttonRepository;
    private final AlertTemplateCategoryRepository categoryRepository;

    public TemplateUpdateConstraint() {
        this(new AlertTemplateRepository(), new AlertButtonRepository(), new AlertTemplateCategoryRepository());
    }

    public TemplateUpdateConstraint(AlertTemplateRepository templateRepository) {
        this(templateRepository, new AlertButtonRepository(), new AlertTemplateCategoryRepository());
    }

    public TemplateUpdateConstraint(AlertTemplateRepository templateRepository, AlertButtonRepository buttonRepository) {
        this(templateRepository, buttonRepository, new AlertTemplateCategoryRepository());
    }

    public TemplateUpdateConstraint(AlertTemplateRepository templateRepository,
                                    AlertButtonRepository buttonRepository,
                                    AlertTemplateCategoryRepository categoryRepository) {
        this.templateRepository = templateRepository;
        this.buttonRepository = buttonRepository;
        this.categoryRepository = categoryRepository;
    }

    public AlertTemplateEntity checkConstraints(TemplateUpdateRequestDTO request) {
        if (request == null) return null;

        AlertTemplateEntity target = null;
        if (request.id() != null) {
            target = templateRepository.get(request.id());
        } else if (request.templateKey() != null && !request.templateKey().isBlank()) {
            target = templateRepository.findByKey(request.templateKey().trim()).orElse(null);
        }

        if (target == null) {
            raiseConstraintError(TemplateConstraintErrors.TEMPLATE_NOT_FOUND);
            return null;
        }

        if (target.getChannel() == AlertChannel.WHATSAPP && request.category() != null && !request.category().isBlank()) {
            if (!isCategoryInRepository(request.category().trim())) {
                raiseConstraintError(TemplateConstraintErrors.TEMPLATE_CATEGORY_NOT_FOUND);
            }
        }

        boolean isWelcomeMenu = InboundWelcomeTemplate.TEMPLATE_KEY.equalsIgnoreCase(target.getTemplateKey())
                || InboundWelcomeTemplate.TEMPLATE_KEY.equalsIgnoreCase(request.templateKey());

        if (request.buttons() != null) {
            if (isWelcomeMenu) {
                boolean hasHuman = request.buttons().stream().anyMatch(b -> Boolean.TRUE.equals(b.requiresHuman()));
                if (!hasHuman) {
                    raiseConstraintError(TemplateConstraintErrors.HUMAN_ESCALATION_BUTTON_REQUIRED);
                }
            }

            for (TemplateButtonDTO btn : request.buttons()) {
                if (btn.type() == ButtonType.QUICK_REPLY && btn.payload() != null && !btn.payload().isBlank()) {
                    Optional<AlertButtonEntity> existingBtn = buttonRepository.findByPayload(btn.payload().trim());
                    if (existingBtn.isPresent()) {
                        AlertButtonEntity existing = existingBtn.get();
                        if (existing.getTemplate() != null && !target.getId().equals(existing.getTemplate().getId())) {
                            raiseConstraintError(TemplateConstraintErrors.BUTTON_PAYLOAD_ALREADY_EXISTS);
                        }
                    }
                }

                boolean isTerminal = btn.requiresHuman()
                        || btn.type() == ButtonType.URL
                        || btn.type() == ButtonType.PHONE_NUMBER;

                if (!isTerminal && btn.replyTemplateKey() != null && !btn.replyTemplateKey().isBlank()) {
                    if (templateRepository.findByKey(btn.replyTemplateKey().trim()).isEmpty()) {
                        raiseConstraintError(TemplateConstraintErrors.TEMPLATE_CHAIN_BROKEN);
                    }
                }
            }
        }

        return target;
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
}
