package com.freelance.alerts.constraint;

import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.AlertEmitRequestDTO;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.keys.alerts.ConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class AlertEmitConstraint extends AbstractConstraint {

    private final AlertTemplateRepository templateRepository;

    public AlertEmitConstraint(AlertTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public AlertTemplateEntity checkConstraints(AlertEmitRequestDTO request) {
        AlertTemplateEntity template = templateRepository.get(request.templateId());
        if (template == null) {
            raiseConstraintError(ConstraintErrors.TEMPLATE_NOT_FOUND);
        }

        if (template.getStatus() != TemplateStatus.APPROVED) {
            raiseConstraintError(ConstraintErrors.TEMPLATE_NOT_APPROVED);
        }

        return template;
    }
}