package com.freelance.alerts;

import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.template.ITemplate;
import com.freelance.event.engine.EventManagementEngine;
import com.freelance.event.model.EventPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class AlertsEngine {

    private static final Logger logger = LoggerFactory.getLogger(AlertsEngine.class);

    private final AlertTemplateRepository templateRepository;

    private static final class Holder {
        private static final AlertsEngine INSTANCE = new AlertsEngine();
    }

    public static AlertsEngine getInstance() {
        return Holder.INSTANCE;
    }

    private AlertsEngine() {
        this(new AlertTemplateRepository());
    }

    public AlertsEngine(AlertTemplateRepository templateRepository) {
        this.templateRepository = templateRepository != null ? templateRepository : new AlertTemplateRepository();
    }

    public void emitAlert(UUID templateId, UUID userId) {
        if (templateId == null || userId == null) return;
        AlertTemplateEntity template = templateRepository.get(templateId);
        EventPriority priority = new AlertEmitService().resolvePriority(template);
        emitAlert(templateId, userId, priority);
    }

    public void emitAlert(UUID templateId, UUID userId, EventPriority priority) {
        if (templateId == null || userId == null) return;
        AlertTemplateEntity template = templateRepository.get(templateId);
        AlertConsumer destination = template != null ? AlertConsumer.fromChannel(template.getChannel()) : AlertConsumer.OUTBOUND_DISPATCH;
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(templateId, userId);
        EventManagementEngine.emit(destination, payload, priority != null ? priority : EventPriority.NORMAL);
    }

    public void emitAlert(ITemplate template, UUID userId) {
        emitAlert(template, userId, null, null, null, null);
    }

    public void emitAlert(ITemplate template, UUID userId, java.util.Map<String, String> customParameters) {
        emitAlert(template, userId, null, null, null, customParameters);
    }

    public void emitAlert(ITemplate template, UUID userId, String mediaUrl, String mediaId, String mediaType) {
        emitAlert(template, userId, mediaUrl, mediaId, mediaType, null);
    }

    public void emitAlert(ITemplate template, UUID userId, String mediaUrl, String mediaId, String mediaType, java.util.Map<String, String> customParameters) {
        if (template == null || userId == null) return;
        EventPriority priority = template.getPriority() != null && !template.getPriority().isBlank()
                ? AlertEmitService.parsePriority(template.getPriority())
                : new AlertEmitService().resolvePriority(template.getCategory());
        UUID templateId = UUID.nameUUIDFromBytes(("tpl:" + template.getTemplateKey()).getBytes());
        AlertConsumer destination = AlertConsumer.fromChannel(template.getChannel());
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(
                templateId,
                userId,
                null,
                null,
                mediaUrl,
                mediaId,
                mediaType,
                customParameters
        );
        EventManagementEngine.emit(destination, payload, priority);
    }

    public void markNotificationsRead(NotificationReadRequestDTO request) {
        if (request == null || request.userId() == null) return;
        EventManagementEngine.emit(AlertConsumer.INBOUND_PUSH, request, EventPriority.HIGH);
    }

    public void markNotificationsRead(UUID userId, List<UUID> messageIds) {
        if (userId == null) return;
        markNotificationsRead(new NotificationReadRequestDTO(userId, messageIds));
    }
}