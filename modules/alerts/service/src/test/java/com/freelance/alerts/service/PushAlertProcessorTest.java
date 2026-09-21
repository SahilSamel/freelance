package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertErrorCode;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.push.PushAlertProcessor;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.alerts.attribute.IDomainAttributeResolver;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.user.UserDTO;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;
import com.freelance.user.read.UserReadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PushAlertProcessor Unit Tests")
class PushAlertProcessorTest {

    static class FakeUserReadService extends UserReadService {
        final Set<UUID> validUsers = new HashSet<>();

        @Override
        public UserReadResponseDTO readUser(UserReadRequestDTO request) {
            if (request != null && request.id() != null && validUsers.contains(request.id())) {
                return new UserReadResponseDTO(new UserDTO(
                        request.id(),
                        "john_doe",
                        "passwordHash",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));
            }
            return null;
        }
    }

    static class FakeTemplateRepository extends AlertTemplateRepository {
        final Map<UUID, AlertTemplateEntity> storage = new HashMap<>();

        @Override
        public AlertTemplateEntity get(UUID id) {
            return storage.get(id);
        }

        @Override
        public Optional<AlertTemplateEntity> findByKey(String key) {
            return storage.values().stream()
                    .filter(t -> t.getTemplateKey().equalsIgnoreCase(key))
                    .findFirst();
        }
    }

    static class FakeMessageRepository extends AlertMessageRepository {
        final List<AlertMessageEntity> persisted = new ArrayList<>();

        @Override
        public void persist(AlertMessageEntity entity) {
            persisted.add(entity);
        }

        @Override
        public List<AlertMessageEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            return persisted.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getChannel() == channel)
                    .toList();
        }
    }

    static class FakeChannelConfigRepository extends AlertChannelConfigRepository {
        final Map<AlertChannel, AlertChannelConfigEntity> storage = new HashMap<>();

        @Override
        public Optional<AlertChannelConfigEntity> findByChannel(AlertChannel channel) {
            return Optional.ofNullable(storage.get(channel));
        }

        @Override
        public boolean isChannelAvailable(AlertChannel channel) {
            AlertChannelConfigEntity c = storage.get(channel);
            return c == null || c.isActive();
        }
    }

    private FakeUserReadService userReadService;
    private FakeTemplateRepository templateRepository;
    private FakeMessageRepository messageRepository;
    private FakeChannelConfigRepository channelConfigRepository;
    private PushAlertProcessor processor;

    private UUID userId;

    @BeforeEach
    void setUp() {
        AttributeResolutionEngine.getInstance().resetForTesting();
        userReadService = new FakeUserReadService();
        templateRepository = new FakeTemplateRepository();
        messageRepository = new FakeMessageRepository();
        channelConfigRepository = new FakeChannelConfigRepository();

        processor = new PushAlertProcessor(
                userReadService,
                templateRepository,
                messageRepository,
                channelConfigRepository
        );

        userId = UUID.randomUUID();
        userReadService.validUsers.add(userId);

        // Register sample resolver
        AttributeResolutionEngine.getInstance().register(new IDomainAttributeResolver() {
            @Override
            public String getDomain() {
                return "USER";
            }

            @Override
            public Set<String> getSupportedAttributes() {
                return Set.of("USER.username", "USER.first_name");
            }

            @Override
            public String getAttributeDescription(String attributeKey) {
                return "User attribute";
            }

            @Override
            public Map<String, String> resolve(UUID uId, Set<String> requestedAttributes) {
                return Map.of("USER.username", "john_doe", "USER.first_name", "John");
            }
        });
    }

    @AfterEach
    void tearDown() {
        AttributeResolutionEngine.getInstance().resetForTesting();
    }

    @Test
    @DisplayName("Process outbound push with raw text content successfully")
    void testProcessOutboundRawContentSuccess() {
        UUID messageId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(null, userId, "Your account status has been updated.", messageId);
        EventMessage<AlertEventPayloadDTO> event = EventMessage.of("alert.outbound.push", payload, EventPriority.HIGH);

        processor.processOutbound(event);

        assertEquals(1, messageRepository.persisted.size());
        AlertMessageEntity msg = messageRepository.persisted.get(0);
        assertEquals(messageId, msg.getId());
        assertEquals(userId, msg.getUserId());
        assertEquals(AlertChannel.PUSH, msg.getChannel());
        assertEquals(AlertDirection.OUTBOUND, msg.getDirection());
        assertEquals("NOTIFICATION", msg.getMessageType());
        assertEquals("Your account status has been updated.", msg.getContent());
        assertEquals(AlertMessageStatus.DELIVERED, msg.getStatus());
        assertNull(msg.getErrorCode());
    }

    @Test
    @DisplayName("Process outbound push with template and dynamic attributes successfully")
    void testProcessOutboundTemplateSuccess() {
        UUID templateId = UUID.randomUUID();
        AlertTemplateEntity tpl = new AlertTemplateEntity(
                templateId,
                "profile_incomplete_reminder",
                "Profile Reminder",
                "REMINDER",
                "en_US",
                "Hi {{USER.first_name}}, please update your username ({{USER.username}}).",
                null,
                null,
                AlertChannel.PUSH,
                TemplateStatus.APPROVED,
                true,
                "USER.first_name,USER.username",
                null,
                "NORMAL"
        );
        templateRepository.storage.put(templateId, tpl);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(templateId, userId);
        EventMessage<AlertEventPayloadDTO> event = EventMessage.of("alert.outbound.push", payload, EventPriority.NORMAL);

        processor.processOutbound(event);

        assertEquals(1, messageRepository.persisted.size());
        AlertMessageEntity msg = messageRepository.persisted.get(0);
        assertEquals(userId, msg.getUserId());
        assertEquals(AlertChannel.PUSH, msg.getChannel());
        assertEquals("TEMPLATE", msg.getMessageType());
        assertEquals("Hi John, please update your username (john_doe).", msg.getContent());
        assertEquals(AlertMessageStatus.DELIVERED, msg.getStatus());
    }

    @Test
    @DisplayName("Non-existent user is dropped cleanly without saving message")
    void testNonExistentUserIsDropped() {
        UUID unknownUserId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(null, unknownUserId, "Hello unknown user", UUID.randomUUID());
        EventMessage<AlertEventPayloadDTO> event = EventMessage.of("alert.outbound.push", payload, EventPriority.NORMAL);

        processor.processOutbound(event);

        assertTrue(messageRepository.persisted.isEmpty());
    }

    @Test
    @DisplayName("Channel paused records FAILED message with CHANNEL_PAUSED error")
    void testChannelPausedRecordsFailed() {
        channelConfigRepository.storage.put(AlertChannel.PUSH, new AlertChannelConfigEntity(AlertChannel.PUSH, false, "Database maintenance"));

        UUID messageId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(null, userId, "Some notification", messageId);
        EventMessage<AlertEventPayloadDTO> event = EventMessage.of("alert.outbound.push", payload, EventPriority.NORMAL);

        processor.processOutbound(event);

        assertEquals(1, messageRepository.persisted.size());
        AlertMessageEntity msg = messageRepository.persisted.get(0);
        assertEquals(AlertMessageStatus.FAILED, msg.getStatus());
        assertEquals(AlertErrorCode.CHANNEL_PAUSED.getCode(), msg.getErrorCode());
    }

    @Test
    @DisplayName("Template channel mismatch records FAILED message with CHANNEL_MISMATCH error")
    void testTemplateChannelMismatchRecordsFailed() {
        UUID templateId = UUID.randomUUID();
        AlertTemplateEntity whatsappTpl = new AlertTemplateEntity(
                templateId,
                "whatsapp_only",
                "WhatsApp Only",
                "UTILITY",
                "en_US",
                "WhatsApp content",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );
        templateRepository.storage.put(templateId, whatsappTpl);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(templateId, userId);
        EventMessage<AlertEventPayloadDTO> event = EventMessage.of("alert.outbound.push", payload, EventPriority.NORMAL);

        processor.processOutbound(event);

        assertEquals(1, messageRepository.persisted.size());
        AlertMessageEntity msg = messageRepository.persisted.get(0);
        assertEquals(AlertMessageStatus.FAILED, msg.getStatus());
        assertEquals(AlertErrorCode.CHANNEL_MISMATCH.getCode(), msg.getErrorCode());
    }

    @Test
    @DisplayName("Template not approved records FAILED message with TEMPLATE_NOT_APPROVED error")
    void testTemplateNotApprovedRecordsFailed() {
        UUID templateId = UUID.randomUUID();
        AlertTemplateEntity pendingTpl = new AlertTemplateEntity(
                templateId,
                "pending_tpl",
                "Pending Template",
                "UTILITY",
                "en_US",
                "Pending content",
                null,
                null,
                AlertChannel.PUSH,
                TemplateStatus.SUBMITTED,
                false,
                null,
                null
        );
        templateRepository.storage.put(templateId, pendingTpl);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(templateId, userId);
        EventMessage<AlertEventPayloadDTO> event = EventMessage.of("alert.outbound.push", payload, EventPriority.NORMAL);

        processor.processOutbound(event);

        assertEquals(1, messageRepository.persisted.size());
        AlertMessageEntity msg = messageRepository.persisted.get(0);
        assertEquals(AlertMessageStatus.FAILED, msg.getStatus());
        assertEquals(AlertErrorCode.TEMPLATE_NOT_APPROVED.getCode(), msg.getErrorCode());
    }
}
