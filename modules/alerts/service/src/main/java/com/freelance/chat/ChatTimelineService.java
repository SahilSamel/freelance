package com.freelance.chat;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.dto.AdminChatReplyRequestDTO;
import com.freelance.alerts.dto.ChatMessageDTO;
import com.freelance.alerts.dto.ChatTimelineRequestDTO;
import com.freelance.alerts.dto.ChatTimelineResponseDTO;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertConsumer;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.event.engine.EventManagementEngine;
import com.freelance.event.model.EventPriority;
import com.freelance.keys.alerts.message.AlertEmitConfigKeys;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.chat.validation.ChatReplyValidation;

import java.time.Instant;
import java.util.*;

public class ChatTimelineService {

    private final AlertMessageRepository messageRepository;
    private final UserContactRepository contactRepository;
    private final WhatsAppDispatcherService dispatcherService;

    public ChatTimelineService() {
        this(new AlertMessageRepository(), new UserContactRepository(), new WhatsAppDispatcherService());
    }

    public ChatTimelineService(AlertMessageRepository messageRepository, UserContactRepository contactRepository, WhatsAppDispatcherService dispatcherService) {
        this.messageRepository = messageRepository;
        this.contactRepository = contactRepository;
        this.dispatcherService = dispatcherService;
    }

    public ChatTimelineResponseDTO getTimeline(ChatTimelineRequestDTO request) {
        if (request == null || request.userId() == null) {
            return new ChatTimelineResponseDTO(null, false, Collections.emptyList());
        }

        AlertChannel channel = request.channel();
        List<AlertMessageEntity> entities = (channel != null)
                ? messageRepository.findByUserAndChannel(request.userId(), channel)
                : messageRepository.findByUserId(request.userId());

        boolean isHumanRequired;
        if (channel != null) {
            isHumanRequired = contactRepository.findByUserAndChannel(request.userId(), channel)
                    .map(UserContactEntity::isHumanSessionActive)
                    .orElse(false);
        } else {
            isHumanRequired = contactRepository.findByUserId(request.userId()).stream()
                    .anyMatch(UserContactEntity::isHumanSessionActive);
        }

        List<ChatMessageDTO> dtos = new ArrayList<>();
        int max = (request.limit() != null && request.limit() > 0) ? request.limit() : Integer.MAX_VALUE;
        for (int i = 0; i < entities.size() && i < max; i++) {
            AlertMessageEntity e = entities.get(i);
            List<TemplateButtonDTO> buttons = null;
            if (e.getTemplate() != null && e.getTemplate().getButtons() != null && !e.getTemplate().getButtons().isEmpty()) {
                buttons = new ArrayList<>();
                for (AlertButtonEntity b : e.getTemplate().getButtons()) {
                    boolean reqHuman = b.getReply() != null && b.getReply().isRequiresHumanAgent();
                    String replyKey = (b.getReply() != null && b.getReply().getReplyTemplate() != null)
                            ? b.getReply().getReplyTemplate().getTemplateKey()
                            : null;
                    buttons.add(new TemplateButtonDTO(
                            b.getButtonText(),
                            b.getButtonType(),
                            b.getPayload(),
                            replyKey,
                            reqHuman,
                            b.getSequenceOrder()
                    ));
                }
            }

            dtos.add(new ChatMessageDTO(
                    i + 1,
                    e.getId(),
                    e.getUserId(),
                    e.getChannel(),
                    e.getDirection(),
                    e.getMessageType(),
                    e.getContent(),
                    buttons,
                    e.getStatus(),
                    e.getTimestamp()
            ));
        }

        return new ChatTimelineResponseDTO(request.userId(), isHumanRequired, dtos);
    }

    public ChatMessageDTO sendStaffReply(AdminChatReplyRequestDTO request) {
        new ChatReplyValidation().validateRequest(request);

        UUID userId = request.userId();
        AlertChannel channel = request.channel() != null
                ? request.channel()
                : ConfigurationEngine.getEnum(AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);

        // Clear active human support session on contact since staff has replied
        contactRepository.findByUserAndChannel(userId, channel).ifPresent(contact -> {
            contact.setHumanSessionExpiresAt(null);
            contactRepository.merge(contact);
        });

        UUID messageId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(null, userId, request.message(), messageId);
        AlertConsumer destination = AlertConsumer.fromChannel(channel);
        EventManagementEngine.emit(destination, payload, EventPriority.HIGH);

        return new ChatMessageDTO(
                messageId,
                userId,
                channel,
                AlertDirection.OUTBOUND,
                "TEXT",
                request.message(),
                AlertMessageStatus.PENDING,
                Instant.now()
        );
    }
}