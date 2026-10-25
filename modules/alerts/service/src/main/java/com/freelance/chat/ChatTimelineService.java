package com.freelance.chat;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.dto.AdminChatReplyRequestDTO;
import com.freelance.alerts.dto.ChatMessageDTO;
import com.freelance.alerts.dto.ChatReadRequestDTO;
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
import com.freelance.chat.validation.ChatTimelineValidation;

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
        new ChatTimelineValidation().validateRequest(request);

        AlertChannel channel = request.channel();
        boolean unreadOnly = Boolean.TRUE.equals(request.unreadOnly());

        List<AlertMessageEntity> entities;
        if (unreadOnly) {
            entities = messageRepository.findUnreadByUserAndChannel(request.userId(), channel);
        } else if (request.limit() != null || request.beforeTimestamp() != null) {
            entities = messageRepository.findRecentByUserAndChannel(request.userId(), channel, request.limit(), request.beforeTimestamp());
        } else {
            entities = messageRepository.findByUserAndChannel(request.userId(), channel);
        }

        boolean isHumanRequired = contactRepository.findByUserAndChannel(request.userId(), channel)
                .map(UserContactEntity::isHumanSessionActive)
                .orElse(false);

        List<ChatMessageDTO> dtos = mapEntitiesToDTOs(entities, null);
        return new ChatTimelineResponseDTO(request.userId(), isHumanRequired, dtos);
    }

    public List<ChatTimelineResponseDTO> getActiveConversations() {
        return getActiveConversations(AlertChannel.WHATSAPP);
    }

    public List<ChatTimelineResponseDTO> getActiveConversations(AlertChannel channel) {
        AlertChannel targetChannel = channel != null ? channel : AlertChannel.WHATSAPP;
        List<AlertMessageEntity> allMessages = messageRepository.findAllByChannel(targetChannel);

        Map<UUID, List<AlertMessageEntity>> messagesByUser = new LinkedHashMap<>();
        for (AlertMessageEntity m : allMessages) {
            if (m.getUserId() != null) {
                messagesByUser.computeIfAbsent(m.getUserId(), k -> new ArrayList<>()).add(m);
            }
        }

        List<UserContactEntity> contacts = contactRepository.findAllContacts();
        Map<UUID, Boolean> humanSessions = new HashMap<>();
        Map<UUID, String> contactPhoneMap = new HashMap<>();
        for (UserContactEntity c : contacts) {
            if (c.getUserId() != null && c.getChannel() == targetChannel) {
                humanSessions.put(c.getUserId(), c.isHumanSessionActive());
                if (c.getContactValue() != null) {
                    contactPhoneMap.put(c.getUserId(), c.getContactValue());
                }
            }
        }

        Set<UUID> allUserIds = new LinkedHashSet<>();
        allUserIds.addAll(messagesByUser.keySet());
        for (Map.Entry<UUID, Boolean> entry : humanSessions.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                allUserIds.add(entry.getKey());
            }
        }

        Map<UUID, com.freelance.user.UserDTO> userMap = new HashMap<>();
        try {
            com.freelance.user.read.UserReadService readService = new com.freelance.user.read.UserReadService();
            List<com.freelance.user.UserDTO> userList = readService.readUsersByIds(new ArrayList<>(allUserIds));
            if (userList != null) {
                for (com.freelance.user.UserDTO u : userList) {
                    if (u != null && u.id() != null) {
                        userMap.put(u.id(), u);
                    }
                }
            }
        } catch (Exception ignored) {}

        List<ChatTimelineResponseDTO> result = new ArrayList<>();
        int conservativeLimit = 20;
        for (UUID userId : allUserIds) {
            List<AlertMessageEntity> entities = messagesByUser.getOrDefault(userId, Collections.emptyList());
            List<AlertMessageEntity> conservativeEntities = entities.size() > conservativeLimit
                    ? entities.subList(entities.size() - conservativeLimit, entities.size())
                    : entities;

            boolean isHumanRequired = humanSessions.getOrDefault(userId, false);
            List<ChatMessageDTO> dtos = mapEntitiesToDTOs(conservativeEntities, null);

            String contactVal = contactPhoneMap.get(userId);
            com.freelance.user.UserDTO userDto = userMap.get(userId);
            String displayName = null;
            if (userDto != null) {
                String first = userDto.firstName() != null ? userDto.firstName().trim() : "";
                String last = userDto.lastName() != null ? userDto.lastName().trim() : "";
                String full = (first + " " + last).trim();
                if (!full.isBlank()) {
                    displayName = full;
                }
            }
            if (displayName == null && contactVal != null) {
                displayName = contactVal;
            }

            result.add(new ChatTimelineResponseDTO(userId, isHumanRequired, dtos, displayName, contactVal));
        }

        return result;
    }

    private List<ChatMessageDTO> mapEntitiesToDTOs(List<AlertMessageEntity> entities, Integer limit) {
        if (entities == null || entities.isEmpty()) return List.of();
        List<ChatMessageDTO> dtos = new ArrayList<>();
        int start = (limit != null && limit > 0 && entities.size() > limit) ? entities.size() - limit : 0;
        int max = entities.size();
        for (int i = start; i < max; i++) {
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
                    e.getTimestamp(),
                    e.getMediaUrl(),
                    e.getMediaId(),
                    e.getMediaType(),
                    e.getReplyToMessageId(),
                    e.getReplyToSender(),
                    e.getReplyToText()
            ));
        }
        return dtos;
    }

    public ChatMessageDTO sendStaffReply(AdminChatReplyRequestDTO request) {
        new ChatReplyValidation().validateRequest(request);

        UUID userId = request.userId();
        AlertChannel channel = request.channel() != null
                ? request.channel()
                : ConfigurationEngine.getEnum(AlertEmitConfigKeys.DEFAULT_ALERT_CHANNEL, AlertChannel.class, AlertChannel.WHATSAPP);

        // Staff replies do NOT clear the human support session; the live session remains active
        // so staff and customer can continue interacting until explicitly resolved.

        UUID messageId = UUID.randomUUID();
        String msgType = (request.mediaType() != null && !request.mediaType().isBlank())
                ? request.mediaType().toUpperCase()
                : ((request.mediaId() != null || request.mediaUrl() != null) ? "IMAGE" : "TEXT");

        String content = request.message();
        if (content == null || content.isBlank()) {
            content = "IMAGE".equalsIgnoreCase(msgType) ? "[Image]" : "[Document]";
        }

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(
                null,
                userId,
                content,
                messageId,
                request.mediaUrl(),
                request.mediaId(),
                msgType
        );
        AlertConsumer destination = AlertConsumer.fromChannel(channel);
        EventManagementEngine.emit(destination, payload, EventPriority.HIGH);

        ChatMessageDTO replyDto = new ChatMessageDTO(
                messageId,
                userId,
                channel,
                AlertDirection.OUTBOUND,
                msgType,
                content,
                AlertMessageStatus.PENDING,
                Instant.now(),
                request.mediaUrl(),
                request.mediaId(),
                msgType
        );

        String tenantId = com.freelance.rest.app.transaction.Transaction.getRequestContext().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = com.freelance.system.SystemHardcoding.DEFAULT_TENANT.asString();
        }
        TenantChatStreamHub.broadcast(tenantId, "CHAT_MESSAGE", replyDto);

        return replyDto;
    }

    public int markConversationAsRead(ChatReadRequestDTO request) {
        if (request == null || request.userId() == null) return 0;
        AlertChannel channel = request.channel() != null ? request.channel() : AlertChannel.WHATSAPP;
        return messageRepository.markAllInboundAsRead(request.userId(), channel);
    }
}