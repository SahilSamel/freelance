package com.freelance.alerts.push;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertErrorCode;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.whatsapp.WhatsAppUtility;
import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.event.model.EventMessage;
import com.freelance.template.utils.TemplateRenderUtility;
import com.freelance.user.read.UserReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Processor for in-app / push notification alerts.
 * Resolves recipient, validates user identity, resolves dynamic template attributes,
 * and persists the delivered notification to {@code FL_ALERT_MESSAGE} for in-app rendering.
 */
public class PushAlertProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PushAlertProcessor.class);
    private static final AlertChannel CHANNEL = AlertChannel.PUSH;

    private final UserReadService userReadService;
    private final AlertTemplateRepository templateRepository;
    private final AlertMessageRepository messageRepository;
    private final AlertChannelConfigRepository channelConfigRepository;

    public PushAlertProcessor() {
        this(
                new UserReadService(),
                new AlertTemplateRepository(),
                new AlertMessageRepository(),
                new AlertChannelConfigRepository()
        );
    }

    public PushAlertProcessor(
            UserReadService userReadService,
            AlertTemplateRepository templateRepository,
            AlertMessageRepository messageRepository,
            AlertChannelConfigRepository channelConfigRepository
    ) {
        this.userReadService = userReadService;
        this.templateRepository = templateRepository;
        this.messageRepository = messageRepository;
        this.channelConfigRepository = channelConfigRepository;
    }

    public void processOutbound(EventMessage<AlertEventPayloadDTO> message) {
        if (message == null) {
            return;
        }

        AlertEventPayloadDTO payload = message.getPayload();
        if (payload == null || payload.userId() == null || payload.templateId() == null) {
            return;
        }

        UUID userId = payload.userId();

        // 1. Channel state gatekeeping
        if (channelConfigRepository != null && !channelConfigRepository.isChannelAvailable(CHANNEL)) {
            String pauseReason = channelConfigRepository.findByChannel(CHANNEL)
                    .map(AlertChannelConfigEntity::getPauseReason)
                    .orElse("Maintenance");
            String reason = AlertErrorCode.CHANNEL_PAUSED.format(CHANNEL, pauseReason);
            logger.warn("[PushAlertProcessor] {}", reason);

            AlertTemplateEntity tpl = templateRepository != null ? templateRepository.get(payload.templateId()) : null;
            String tplContent = tpl != null ? tpl.getContent() : "[Unknown content]";
            recordFailedMessage(payload, userId, tpl, tplContent, AlertErrorCode.CHANNEL_PAUSED.getCode(), reason);
            return;
        }

        // 2. User existence verification
        if (!WhatsAppUtility.isUserValid(userId, userReadService)) {
            logger.debug("[PushAlertProcessor] User {} does not exist in system. Dropping alert.", userId);
            return;
        }

        // 3. Template & Attribute Resolution
        AlertTemplateEntity template = templateRepository != null ? templateRepository.get(payload.templateId()) : null;
        if (template == null) {
            logger.warn("[PushAlertProcessor] Template with ID {} not found.", payload.templateId());
            return;
        }

        if (template.getChannel() != CHANNEL) {
            String reason = AlertErrorCode.CHANNEL_MISMATCH.format(template.getChannel(), CHANNEL);
            recordFailedMessage(payload, userId, template, template.getContent(), AlertErrorCode.CHANNEL_MISMATCH.getCode(), reason);
            return;
        }

        if (template.getStatus() != TemplateStatus.APPROVED) {
            String reason = AlertErrorCode.TEMPLATE_NOT_APPROVED.format(template.getTemplateKey(), template.getStatus());
            recordFailedMessage(payload, userId, template, template.getContent(), AlertErrorCode.TEMPLATE_NOT_APPROVED.getCode(), reason);
            return;
        }

        Set<String> requiredAttrs = WhatsAppUtility.parseAttributes(template.getRequiredAttributes());
        Map<String, String> resolvedAttributes = AttributeResolutionEngine.getInstance().resolveAll(userId, requiredAttrs);
        String content = TemplateRenderUtility.render(template, resolvedAttributes);

        // 4. Record as DELIVERED in FL_ALERT_MESSAGE for in-app timeline polling
        UUID messageId = payload.messageId() != null ? payload.messageId() : UUID.randomUUID();

        AlertMessageEntity entity = new AlertMessageEntity(
                messageId,
                userId,
                CHANNEL,
                AlertDirection.OUTBOUND,
                "TEMPLATE",
                content,
                template,
                null,
                AlertMessageStatus.DELIVERED,
                null,
                null,
                Instant.now()
        );

        if (messageRepository != null) {
            messageRepository.persist(entity);
        }
    }

    public void processInbound(EventMessage<NotificationReadRequestDTO> message) {
        if (message == null || message.getPayload() == null) {
            return;
        }

        NotificationReadRequestDTO payload = message.getPayload();
        if (payload.userId() == null || messageRepository == null) {
            return;
        }

        UUID userId = payload.userId();
        AlertChannel channel = payload.channel() != null ? payload.channel() : CHANNEL;
        List<UUID> messageIds = payload.messageIds();

        if (messageIds != null && !messageIds.isEmpty()) {
            for (UUID msgId : messageIds) {
                if (msgId == null) continue;
                AlertMessageEntity entity = messageRepository.get(msgId);
                if (entity != null && userId.equals(entity.getUserId())) {
                    if (entity.getStatus() != AlertMessageStatus.READ) {
                        entity.setStatus(AlertMessageStatus.READ);
                        messageRepository.merge(entity);
                        logger.info("[PushAlertProcessor] Marked message {} as READ for user {}", msgId, userId);
                    }
                }
            }
        } else {
            List<AlertMessageEntity> unreadMessages = messageRepository.findUnreadByUserAndChannel(userId, channel);
            for (AlertMessageEntity entity : unreadMessages) {
                entity.setStatus(AlertMessageStatus.READ);
                messageRepository.merge(entity);
            }
            logger.info("[PushAlertProcessor] Marked {} unread messages as READ for user {} on channel {}",
                    unreadMessages.size(), userId, channel);
        }
    }

    private void recordFailedMessage(
            AlertEventPayloadDTO payload,
            UUID userId,
            AlertTemplateEntity template,
            String content,
            String errorCode,
            String errorReason
    ) {
        if (messageRepository == null) return;
        UUID messageId = payload.messageId() != null ? payload.messageId() : UUID.randomUUID();
        String messageType = template != null ? "TEMPLATE" : "NOTIFICATION";
        String resolvedContent = content != null ? content : (template != null ? template.getContent() : "[Unknown content]");

        AlertMessageEntity failedEntity = new AlertMessageEntity(
                messageId,
                userId,
                CHANNEL,
                AlertDirection.OUTBOUND,
                messageType,
                resolvedContent,
                template,
                null,
                AlertMessageStatus.FAILED,
                errorCode,
                errorReason,
                Instant.now()
        );
        messageRepository.persist(failedEntity);
    }
}
