package com.freelance.alerts;

import com.freelance.alerts.dto.AlertEmitRequestDTO;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.constraint.AlertEmitConstraint;
import com.freelance.alerts.validation.AlertEmitValidation;
import com.freelance.event.engine.EventManagementEngine;
import com.freelance.event.model.EventPriority;

import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AlertEmitService {

    private final AlertTemplateCategoryRepository categoryRepository;
    private final AlertTemplateRepository templateRepository;

    public AlertEmitService() {
        this(new AlertTemplateRepository(), new AlertTemplateCategoryRepository());
    }

    public AlertEmitService(AlertTemplateRepository templateRepository, AlertTemplateCategoryRepository alertTemplateCategoryRepository) {
        this.templateRepository = templateRepository;
        this.categoryRepository = alertTemplateCategoryRepository;
    }

    public void emitBatchAlert(AlertEmitRequestDTO request) {
        new AlertEmitValidation().validateRequest(request);
        AlertTemplateEntity template = new AlertEmitConstraint(templateRepository).checkConstraints(request);

        EventPriority priority = resolvePriority(template);

        AlertConsumer destination = AlertConsumer.fromChannel(template.getChannel());
        List<AlertEventPayloadDTO> payloads = new ArrayList<>(request.userIds().size());
        for (UUID userId : request.userIds()) {
            payloads.add(new AlertEventPayloadDTO(template.getId(), userId));
        }
        EventManagementEngine.emitBatch(destination, payloads, priority);
    }

    public EventPriority resolvePriority(AlertTemplateEntity template) {
        if (template == null) {
            return EventPriority.NORMAL;
        }
        if (template.getPriority() != null && !template.getPriority().isBlank()) {
            return parsePriority(template.getPriority());
        }
        return resolvePriority(template.getCategory());
    }

    public EventPriority resolvePriority(String category) {
        if (category == null || category.isBlank()) {
            return EventPriority.NORMAL;
        }
        try {
            return categoryRepository.findByKey(category)
                    .map(c -> parsePriority(c.getPriority()))
                    .orElse(EventPriority.NORMAL);
        } catch (Exception e) {
            return EventPriority.NORMAL;
        }
    }

    public static EventPriority parsePriority(String priorityStr) {
        if (priorityStr == null || priorityStr.isBlank()) {
            return EventPriority.NORMAL;
        }
        try {
            return EventPriority.valueOf(priorityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return EventPriority.NORMAL;
        }
    }
}