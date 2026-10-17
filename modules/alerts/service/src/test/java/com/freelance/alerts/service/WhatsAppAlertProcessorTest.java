package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertErrorCode;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.AlertsEngine;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.dto.webhook.*;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonReplyRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.whatsapp.WhatsAppAlertProcessor;
import com.freelance.dispatcher.AlertDispatcherFactory;
import com.freelance.dispatcher.IAlertDispatcher;
import com.freelance.dispatcher.data.AlertDispatchRequest;
import com.freelance.dispatcher.data.AlertDispatchResult;
import com.freelance.event.exception.EventDelayedException;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WhatsAppAlertProcessor Comprehensive Unit Tests")
class WhatsAppAlertProcessorTest {

    static class FakeUserContactRepository extends UserContactRepository {
        final Map<UUID, UserContactEntity> byId = new HashMap<>();
        final Map<String, UserContactEntity> byContact = new HashMap<>();
        final Map<String, UserContactEntity> byUserAndChannel = new HashMap<>();

        @Override
        public Optional<UserContactEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            if (userId == null || channel == null) return Optional.empty();
            return Optional.ofNullable(byUserAndChannel.get(userId + ":" + channel));
        }

        @Override
        public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
            if (contactValue == null || channel == null) return Optional.empty();
            String key = contactValue.trim() + ":" + channel;
            if (byContact.containsKey(key)) return Optional.of(byContact.get(key));

            String alt = contactValue.startsWith("+") ? contactValue.substring(1) : "+" + contactValue;
            String altKey = alt + ":" + channel;
            return Optional.ofNullable(byContact.get(altKey));
        }

        @Override
        public void persist(UserContactEntity entity) {
            byId.put(entity.getId(), entity);
            byContact.put(entity.getContactValue() + ":" + entity.getChannel(), entity);
            byUserAndChannel.put(entity.getUserId() + ":" + entity.getChannel(), entity);
        }

        @Override
        public UserContactEntity merge(UserContactEntity entity) {
            persist(entity);
            return entity;
        }
    }

    static class FakeAlertMessageRepository extends AlertMessageRepository {
        final Map<UUID, AlertMessageEntity> byId = new HashMap<>();
        final Map<String, AlertMessageEntity> byMetaId = new HashMap<>();
        final List<AlertMessageEntity> messages = new ArrayList<>();

        @Override
        public void persist(AlertMessageEntity entity) {
            byId.put(entity.getId(), entity);
            if (entity.getMetaMessageId() != null) {
                byMetaId.put(entity.getMetaMessageId(), entity);
            }
            messages.add(entity);
        }

        @Override
        public AlertMessageEntity merge(AlertMessageEntity entity) {
            persist(entity);
            return entity;
        }

        @Override
        public Optional<AlertMessageEntity> findByMetaMessageId(String metaMessageId) {
            if (metaMessageId == null) return Optional.empty();
            return Optional.ofNullable(byMetaId.get(metaMessageId.trim()));
        }

        @Override
        public Optional<AlertMessageEntity> findLatestByUserAndChannel(UUID userId, AlertChannel channel) {
            return messages.stream()
                    .filter(m -> userId.equals(m.getUserId()) && channel == m.getChannel())
                    .reduce((first, second) -> second);
        }
    }

    static class FakeAlertChannelConfigRepository extends AlertChannelConfigRepository {
        final Map<AlertChannel, AlertChannelConfigEntity> configs = new HashMap<>();

        @Override
        public Optional<AlertChannelConfigEntity> findByChannel(AlertChannel channel) {
            return Optional.ofNullable(configs.get(channel));
        }

        @Override
        public boolean isChannelAvailable(AlertChannel channel) {
            AlertChannelConfigEntity cfg = configs.get(channel);
            return cfg == null || cfg.isActive();
        }

        @Override
        public AlertChannelConfigEntity get(AlertChannel channel) {
            return configs.get(channel);
        }
    }

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<UUID, AlertTemplateEntity> byId = new HashMap<>();
        final Map<String, AlertTemplateEntity> byKey = new HashMap<>();
        final Map<String, AlertTemplateEntity> byMetaId = new HashMap<>();

        @Override
        public AlertTemplateEntity get(UUID id) {
            return byId.get(id);
        }

        @Override
        public Optional<AlertTemplateEntity> findByKey(String key) {
            if (key == null) return Optional.empty();
            return Optional.ofNullable(byKey.get(key));
        }

        @Override
        public Optional<AlertTemplateEntity> findByKeyIgnoreCase(String key) {
            if (key == null) return Optional.empty();
            return byKey.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(key))
                    .map(Map.Entry::getValue)
                    .findFirst();
        }

        @Override
        public Optional<AlertTemplateEntity> findByMetaTemplateId(String metaId) {
            if (metaId == null) return Optional.empty();
            return Optional.ofNullable(byMetaId.get(metaId));
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            byId.put(entity.getId(), entity);
            byKey.put(entity.getTemplateKey(), entity);
            if (entity.getMetaTemplateId() != null) {
                byMetaId.put(entity.getMetaTemplateId(), entity);
            }
        }

        @Override
        public AlertTemplateEntity merge(AlertTemplateEntity entity) {
            persist(entity);
            return entity;
        }
    }

    static class FakeAlertButtonReplyRepository extends AlertButtonReplyRepository {
        final Map<String, AlertButtonReplyEntity> byPayload = new HashMap<>();

        @Override
        public Optional<AlertButtonReplyEntity> findByButtonPayload(String payload) {
            if (payload == null) return Optional.empty();
            return Optional.ofNullable(byPayload.get(payload));
        }
    }

    static class FakeDispatcherFactory extends AlertDispatcherFactory {
        IAlertDispatcher customDispatcher;

        @Override
        public Optional<IAlertDispatcher> getDispatcher(AlertChannel channel) {
            return Optional.ofNullable(customDispatcher);
        }
    }

    static class FakeUserReadService extends com.freelance.user.read.UserReadService {
        final Set<UUID> validUsers = new HashSet<>();

        @Override
        public com.freelance.user.read.UserReadResponseDTO readUser(com.freelance.user.read.UserReadRequestDTO request) {
            if (request != null && request.id() != null && validUsers.contains(request.id())) {
                return new com.freelance.user.read.UserReadResponseDTO(
                        new com.freelance.user.UserDTO(request.id(), "testuser", "pass", java.time.LocalDateTime.now(), java.time.LocalDateTime.now())
                );
            }
            throw new com.freelance.rest.app.service.exception.constraint.ConstraintException(com.freelance.keys.identityManagement.user.UserConstraintErrors.USER_NOT_FOUND);
        }
    }

    private TransactionContext context;
    private FakeUserReadService userReadService;
    private FakeUserContactRepository contactRepo;
    private FakeAlertMessageRepository messageRepo;
    private FakeAlertChannelConfigRepository channelConfigRepo;
    private FakeAlertTemplateRepository templateRepo;
    private FakeAlertButtonReplyRepository buttonReplyRepo;
    private FakeDispatcherFactory dispatcherFactory;
    private WhatsAppAlertProcessor processor;

    @BeforeEach
    void setUp() {
        com.freelance.alerts.whatsapp.WhatsAppUtility.resumeQueue();
        context = TestContextHelper.setUp();
        userReadService = new FakeUserReadService();
        contactRepo = new FakeUserContactRepository();
        messageRepo = new FakeAlertMessageRepository();
        channelConfigRepo = new FakeAlertChannelConfigRepository();
        templateRepo = new FakeAlertTemplateRepository();
        buttonReplyRepo = new FakeAlertButtonReplyRepository();
        dispatcherFactory = new FakeDispatcherFactory();

        processor = new WhatsAppAlertProcessor(
                userReadService,
                templateRepo,
                contactRepo,
                channelConfigRepo,
                messageRepo,
                buttonReplyRepo,
                dispatcherFactory,
                AlertsEngine.getInstance()
        );
    }

    @AfterEach
    void tearDown() {
        com.freelance.alerts.whatsapp.WhatsAppUtility.resumeQueue();
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Constructors initialize cleanly")
    void testConstructors() {
        assertNotNull(new WhatsAppAlertProcessor());
        assertNotNull(new WhatsAppAlertProcessor(null, templateRepo, contactRepo, channelConfigRepo, messageRepo, dispatcherFactory));
        assertNotNull(new WhatsAppAlertProcessor(messageRepo, contactRepo, channelConfigRepo, buttonReplyRepo, templateRepo, null));
    }

    @Test
    @DisplayName("processOutbound null / invalid payloads are safely ignored")
    void testOutboundNullPayloads() {
        assertDoesNotThrow(() -> processor.processOutbound(null));

        EventMessage<AlertEventPayloadDTO> nullPayloadMsg = EventMessage.of(
                () -> "test.queue",
                null
        );
        assertDoesNotThrow(() -> processor.processOutbound(nullPayloadMsg));

        EventMessage<AlertEventPayloadDTO> noUserMsg = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(UUID.randomUUID(), null, "Content", UUID.randomUUID())
        );
        assertDoesNotThrow(() -> processor.processOutbound(noUserMsg));
    }

    @Test
    @DisplayName("processOutbound records FAILED and DLQ when channel is paused/disabled")
    void testOutboundChannelPaused() {
        channelConfigRepo.configs.put(AlertChannel.WHATSAPP, new AlertChannelConfigEntity(AlertChannel.WHATSAPP, false, "System maintenance"));

        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        userReadService.validUsers.add(userId);
        EventMessage<AlertEventPayloadDTO> msg = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(null, userId, "Hello", messageId)
        );

        processor.processOutbound(msg);

        assertEquals(2, messageRepo.messages.size());
        AlertMessageEntity recorded = messageRepo.messages.get(0);
        assertEquals(AlertMessageStatus.FAILED, recorded.getStatus());
        assertEquals(AlertErrorCode.CHANNEL_PAUSED.getCode(), recorded.getErrorCode());
    }

    @Test
    @DisplayName("processOutbound records FAILED when contact is missing, opted-out, or inactive")
    void testOutboundContactStates() {
        UUID userId = UUID.randomUUID();
        userReadService.validUsers.add(userId);
        UUID tplId = UUID.randomUUID();
        AlertTemplateEntity tpl = new AlertTemplateEntity(tplId, "TPL_TEST", "Test", "MARKETING", "en", "Content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null);
        templateRepo.persist(tpl);

        // 1. Missing contact
        EventMessage<AlertEventPayloadDTO> msg1 = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(tplId, userId, null, UUID.randomUUID())
        );
        processor.processOutbound(msg1);
        assertEquals(1, messageRepo.messages.size());
        assertEquals(AlertErrorCode.NO_CONTACT_FOUND.getCode(), messageRepo.messages.get(0).getErrorCode());

        // 2. Opted out contact
        UserContactEntity optedOut = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+123456", true, true);
        contactRepo.persist(optedOut);

        processor.processOutbound(msg1);
        assertEquals(2, messageRepo.messages.size());
        assertEquals(AlertErrorCode.USER_OPTED_OUT.getCode(), messageRepo.messages.get(1).getErrorCode());

        // 3. Inactive contact
        optedOut.setOptedOut(false);
        optedOut.setActive(false);
        contactRepo.persist(optedOut);

        processor.processOutbound(msg1);
        assertEquals(3, messageRepo.messages.size());
        assertEquals(AlertErrorCode.CHANNEL_INACTIVE.getCode(), messageRepo.messages.get(2).getErrorCode());
    }

    @Test
    @DisplayName("processOutbound throws EventDelayedException when human session is active")
    void testOutboundHumanSessionActiveThrowsDelayed() {
        UUID userId = UUID.randomUUID();
        userReadService.validUsers.add(userId);
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+123456", false, true, Instant.now().plus(Duration.ofHours(5)));
        contactRepo.persist(contact);

        EventMessage<AlertEventPayloadDTO> msg = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(null, userId, "Normal priority alert", UUID.randomUUID()),
                EventPriority.NORMAL
        );

        assertThrows(EventDelayedException.class, () -> processor.processOutbound(msg));
    }

    @Test
    @DisplayName("processOutbound records FAILED on template mismatch, unapproved template, or missing dispatcher")
    void testOutboundTemplateAndDispatcherIssues() {
        UUID userId = UUID.randomUUID();
        userReadService.validUsers.add(userId);
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+123456", false, true);
        contactRepo.persist(contact);

        // 1. Template not approved
        UUID unapprovedId = UUID.randomUUID();
        AlertTemplateEntity unapproved = new AlertTemplateEntity(unapprovedId, "TPL_DRAFT", "Draft", "UTILITY", "en", "Content", null, null, AlertChannel.WHATSAPP, TemplateStatus.DRAFT, false, null, null);
        templateRepo.persist(unapproved);

        EventMessage<AlertEventPayloadDTO> msg1 = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(unapprovedId, userId, null, UUID.randomUUID())
        );
        processor.processOutbound(msg1);
        assertEquals(AlertErrorCode.TEMPLATE_NOT_APPROVED.getCode(), messageRepo.messages.get(0).getErrorCode());

        // 2. Approved template but missing dispatcher
        unapproved.setStatus(TemplateStatus.APPROVED);
        templateRepo.persist(unapproved);

        processor.processOutbound(msg1);
        assertEquals(AlertErrorCode.CHANNEL_NOT_AVAILABLE.getCode(), messageRepo.messages.get(1).getErrorCode());
    }

    @Test
    @DisplayName("processOutbound executes dispatch and handles success, failure, and delayed results")
    void testOutboundDispatchOutcomes() {
        UUID userId = UUID.randomUUID();
        userReadService.validUsers.add(userId);
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+123456", false, true);
        contactRepo.persist(contact);

        UUID tplId = UUID.randomUUID();
        AlertTemplateEntity tpl = new AlertTemplateEntity(tplId, "TPL_OK", "OK", "UTILITY", "en", "Hello", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null);
        templateRepo.persist(tpl);

        // 1. Success dispatch
        dispatcherFactory.customDispatcher = new IAlertDispatcher() {
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public AlertDispatchResult dispatch(AlertDispatchRequest req) {
                return AlertDispatchResult.success("wamid.success.123", "{\"status\":\"ok\"}");
            }
        };
        EventMessage<AlertEventPayloadDTO> msg1 = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(tplId, userId, null, UUID.randomUUID())
        );
        processor.processOutbound(msg1);
        assertEquals(AlertMessageStatus.SENT, messageRepo.messages.get(0).getStatus());
        assertEquals("wamid.success.123", messageRepo.messages.get(0).getMetaMessageId());

        // 2. Failure dispatch
        dispatcherFactory.customDispatcher = new IAlertDispatcher() {
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public AlertDispatchResult dispatch(AlertDispatchRequest req) {
                return AlertDispatchResult.failure("ERR_LIMIT", "Rate limit hit", "{}");
            }
        };
        EventMessage<AlertEventPayloadDTO> msg2 = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(tplId, userId, null, UUID.randomUUID())
        );
        processor.processOutbound(msg2);
        assertEquals(AlertMessageStatus.FAILED, messageRepo.messages.get(1).getStatus());
        assertEquals("ERR_LIMIT", messageRepo.messages.get(1).getErrorCode());

        // 3. Delayed dispatch
        dispatcherFactory.customDispatcher = new IAlertDispatcher() {
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public AlertDispatchResult dispatch(AlertDispatchRequest req) {
                return AlertDispatchResult.delayed("Temporary throttle");
            }
        };
        assertThrows(EventDelayedException.class, () -> processor.processOutbound(msg2));
    }

    @Test
    @DisplayName("processInbound processes status updates (delivered, read, sent, failed, error)")
    void testInboundStatusReceipts() {
        String wamid = "wamid.status.test";
        UUID userId = UUID.randomUUID();
        AlertMessageEntity sentMsg = new AlertMessageEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.OUTBOUND, "TEMPLATE", "Text", wamid, AlertMessageStatus.SENT, null, null, Instant.now());
        messageRepo.persist(sentMsg);

        // Delivered status
        WebhookStatusDTO deliveredStatus = new WebhookStatusDTO(wamid, "delivered", "1700000000", null, Collections.emptyList());
        WebhookValueDTO value1 = new WebhookValueDTO("whatsapp", null, List.of(deliveredStatus), null, null);
        WhatsAppWebhookPayloadDTO payload1 = new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", value1)))));

        processor.processInbound(EventMessage.of(() -> "inbound", payload1));
        assertEquals(AlertMessageStatus.DELIVERED, sentMsg.getStatus());

        // Read status
        WebhookStatusDTO readStatus = new WebhookStatusDTO(wamid, "read", "1700000005", null, Collections.emptyList());
        WebhookValueDTO value2 = new WebhookValueDTO("whatsapp", null, List.of(readStatus), null, null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", value2)))))));
        assertEquals(AlertMessageStatus.READ, sentMsg.getStatus());

        // Error status
        WebhookErrorDTO errorDto = new WebhookErrorDTO(131056, "Rate limit", "Rate limit exceeded", 0, null);
        WebhookStatusDTO errorStatus = new WebhookStatusDTO(wamid, "failed", "1700000010", null, List.of(errorDto));
        WebhookValueDTO value3 = new WebhookValueDTO("whatsapp", null, List.of(errorStatus), null, null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", value3)))))));
        assertEquals(AlertMessageStatus.FAILED, sentMsg.getStatus());
    }

    @Test
    @DisplayName("processInbound processes customer text, opt-out keywords, and button replies")
    void testInboundCustomerMessages() {
        String phone = "+919988776655";
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, phone, false, true);
        contactRepo.persist(contact);

        // 1. Opt-out keyword "STOP"
        WebhookMessageDTO stopMsg = new WebhookMessageDTO(phone, "msg-1", "1700000000", "text", new WebhookTextMessageDTO("STOP"), null, null);
        WebhookValueDTO val1 = new WebhookValueDTO("whatsapp", null, null, List.of(stopMsg), null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", val1)))))));

        assertTrue(contact.isOptedOut());
        assertFalse(contact.isActive());

        // 2. Opt-in keyword "START"
        WebhookMessageDTO startMsg = new WebhookMessageDTO(phone, "msg-2", "1700000001", "text", new WebhookTextMessageDTO("START"), null, null);
        WebhookValueDTO val2 = new WebhookValueDTO("whatsapp", null, null, List.of(startMsg), null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", val2)))))));

        assertFalse(contact.isOptedOut());
        assertTrue(contact.isActive());

        // 3. Button reply requiring human agent
        AlertButtonReplyEntity replyEntity = new AlertButtonReplyEntity(UUID.randomUUID(), null, null, true);
        buttonReplyRepo.byPayload.put("PAYLOAD_SUPPORT", replyEntity);

        WebhookInteractiveDTO interactive = new WebhookInteractiveDTO("button_reply", new WebhookButtonReplyDTO("PAYLOAD_SUPPORT", "Talk to Agent"), null);
        WebhookMessageDTO btnMsg = new WebhookMessageDTO(phone, "msg-3", "1700000002", "interactive", null, interactive, null);
        WebhookValueDTO val3 = new WebhookValueDTO("whatsapp", null, null, List.of(btnMsg), null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", val3)))))));

        assertTrue(contact.isHumanSessionActive());
    }

    @Test
    @DisplayName("processInbound handles Meta template approval webhook updates")
    void testInboundTemplateStatusUpdates() {
        AlertTemplateEntity tpl = new AlertTemplateEntity(UUID.randomUUID(), "tpl_marketing_1", "Marketing", "UTILITY", "en", "Text", null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null);
        templateRepo.persist(tpl);

        WebhookTemplateStatusDTO tplStatus = new WebhookTemplateStatusDTO("APPROVED", "meta_tpl_999", "tpl_marketing_1", "en", null);
        WebhookValueDTO val = new WebhookValueDTO(
                "whatsapp", null, null, null, tplStatus, null, "APPROVED", "meta_tpl_999", "tpl_marketing_1", "en", null
        );

        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("message_template_status_update", val)))))));

        assertEquals(TemplateStatus.APPROVED, tpl.getStatus());
        assertEquals("meta_tpl_999", tpl.getMetaTemplateId());
    }

    @Test
    @DisplayName("handleOutboundDeadLetter records DLQ message on permanent and delayed errors")
    void testDeadLetterHandling() {
        UUID userId = UUID.randomUUID();
        EventMessage<AlertEventPayloadDTO> msg = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(null, userId, "Dead letter content", UUID.randomUUID())
        );

        // Max retries exceeded
        processor.handleOutboundDeadLetter(msg, new RuntimeException("Downstream network timeout"));
        assertEquals(1, messageRepo.messages.size());
        assertEquals(AlertErrorCode.MAX_RETRIES_EXCEEDED.getCode(), messageRepo.messages.get(0).getErrorCode());

        // Human reply timeout
        processor.handleOutboundDeadLetter(msg, new EventDelayedException("Human reply expired"));
        assertEquals(2, messageRepo.messages.size());
        assertEquals(AlertErrorCode.HUMAN_REPLY_TIMEOUT.getCode(), messageRepo.messages.get(1).getErrorCode());
    }

    @Test
    @DisplayName("processInbound handles null, empty payload, empty entries, delivered and sent statuses")
    void testProcessInboundEdgeCases() {
        assertDoesNotThrow(() -> processor.processInbound(null));
        assertDoesNotThrow(() -> processor.processInbound(EventMessage.of(() -> "inbound", null)));
        assertDoesNotThrow(() -> processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO(null, null))));
        assertDoesNotThrow(() -> processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("obj", Collections.singletonList(null)))));

        // Sent status
        String wamid = "wamid.sent.test";
        AlertMessageEntity sentMsg = new AlertMessageEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TPL", "Text", wamid, AlertMessageStatus.PENDING, null, null, Instant.now()
        );
        messageRepo.persist(sentMsg);

        WebhookStatusDTO sentStatus = new WebhookStatusDTO(wamid, "sent", "1700000001", null, null);
        WebhookValueDTO valSent = new WebhookValueDTO("whatsapp", null, List.of(sentStatus), null, null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", valSent)))))));
        assertEquals(AlertMessageStatus.SENT, sentMsg.getStatus());

        // Delivered status
        WebhookStatusDTO delStatus = new WebhookStatusDTO(wamid, "delivered", "1700000002", null, null);
        WebhookValueDTO valDel = new WebhookValueDTO("whatsapp", null, List.of(delStatus), null, null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", valDel)))))));
        assertEquals(AlertMessageStatus.DELIVERED, sentMsg.getStatus());
    }

    @Test
    @DisplayName("processInbound handles rejected and paused template status updates")
    void testRejectedAndPausedTemplateUpdates() {
        AlertTemplateEntity tpl = new AlertTemplateEntity(UUID.randomUUID(), "tpl_pause_rej", "Test", "UTILITY", "en", "Text", null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null);
        templateRepo.persist(tpl);

        // Rejected
        WebhookTemplateStatusDTO rejStatus = new WebhookTemplateStatusDTO("REJECTED", "meta_rej_1", "tpl_pause_rej", "en", "Content policy");
        WebhookValueDTO valRej = new WebhookValueDTO("whatsapp", null, null, null, rejStatus, null, "REJECTED", "meta_rej_1", "tpl_pause_rej", "en", "Content policy");
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("message_template_status_update", valRej)))))));
        assertEquals(TemplateStatus.REJECTED, tpl.getStatus());

        // Paused
        WebhookTemplateStatusDTO pauseStatus = new WebhookTemplateStatusDTO("PAUSED", "meta_rej_1", "tpl_pause_rej", "en", "High block rate");
        WebhookValueDTO valPause = new WebhookValueDTO("whatsapp", null, null, null, pauseStatus, null, "PAUSED", "meta_rej_1", "tpl_pause_rej", "en", "High block rate");
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("message_template_status_update", valPause)))))));
        assertEquals(TemplateStatus.PAUSED, tpl.getStatus());
    }

    @Test
    @DisplayName("processInbound handles native WhatsApp userPreferences opt-out and opt-in")
    void testUserPreferencesOptOutAndIn() {
        String phone = "+919123456780";

        // Opt out via preference webhook
        WebhookUserPreferenceDTO optOutPref = new WebhookUserPreferenceDTO(phone, null, null, "OPT_OUT", null, null, "1700000000");
        WebhookValueDTO val1 = new WebhookValueDTO("whatsapp", null, null, null, null, List.of(optOutPref), null, null, null, null, null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("user_preferences", val1)))))));

        Optional<UserContactEntity> c1 = contactRepo.findByContactAndChannel(phone, AlertChannel.WHATSAPP);
        assertTrue(c1.isPresent());
        assertTrue(c1.get().isOptedOut());

        // Opt in via preference webhook
        WebhookUserPreferenceDTO optInPref = new WebhookUserPreferenceDTO(phone, null, null, "OPT_IN", null, null, "1700000001");
        WebhookValueDTO val2 = new WebhookValueDTO("whatsapp", null, null, null, null, List.of(optInPref), null, null, null, null, null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("user_preferences", val2)))))));

        Optional<UserContactEntity> c2 = contactRepo.findByContactAndChannel(phone, AlertChannel.WHATSAPP);
        assertTrue(c2.isPresent());
        assertFalse(c2.get().isOptedOut());
    }

    @Test
    @DisplayName("processOutbound throws IllegalStateException when queue is paused")
    void testOutboundQueuePaused() {
        try {
            com.freelance.alerts.whatsapp.WhatsAppUtility.pauseQueue();
            UUID userId = UUID.randomUUID();
            userReadService.validUsers.add(userId);
            EventMessage<AlertEventPayloadDTO> msg = EventMessage.of(
                    () -> "test.queue",
                    new AlertEventPayloadDTO(null, userId, "Content", UUID.randomUUID())
            );
            assertThrows(IllegalStateException.class, () -> processor.processOutbound(msg));
        } finally {
            com.freelance.alerts.whatsapp.WhatsAppUtility.resumeQueue();
        }
    }

    @Test
    @DisplayName("processOutbound drops message gracefully when user is invalid")
    void testOutboundInvalidUserDropped() {
        UUID invalidUserId = UUID.randomUUID();
        EventMessage<AlertEventPayloadDTO> msg = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(null, invalidUserId, "Content", UUID.randomUUID())
        );
        processor.processOutbound(msg);
        assertTrue(messageRepo.messages.isEmpty());
    }

    @Test
    @DisplayName("processOutbound records SESSION_WINDOW_CLOSED when template has > 3 buttons outside 24h interactive window")
    void testOutboundInteractiveSessionWindowClosed() {
        UUID userId = UUID.randomUUID();
        userReadService.validUsers.add(userId);
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+123456", false, true);
        contactRepo.persist(contact);

        UUID tplId = UUID.randomUUID();
        AlertTemplateEntity tpl = new AlertTemplateEntity(tplId, "TPL_BUTTONS", "Buttons", "UTILITY", "en", "Content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null);
        List<com.freelance.alerts.template.entity.AlertButtonEntity> buttons = List.of(
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), tpl, "B1", com.freelance.alerts.ButtonType.QUICK_REPLY, "P1", 1),
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), tpl, "B2", com.freelance.alerts.ButtonType.QUICK_REPLY, "P2", 2),
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), tpl, "B3", com.freelance.alerts.ButtonType.QUICK_REPLY, "P3", 3),
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), tpl, "B4", com.freelance.alerts.ButtonType.QUICK_REPLY, "P4", 4)
        );
        tpl.setButtons(buttons);
        templateRepo.persist(tpl);

        EventMessage<AlertEventPayloadDTO> msg = EventMessage.of(
                () -> "test.queue",
                new AlertEventPayloadDTO(tplId, userId, null, UUID.randomUUID())
        );

        processor.processOutbound(msg);
        assertEquals(1, messageRepo.messages.size());
        assertEquals(AlertErrorCode.SESSION_WINDOW_CLOSED.getCode(), messageRepo.messages.get(0).getErrorCode());
    }

    @Test
    @DisplayName("processInbound handles template status update with field fallback and unmapped button reply")
    void testInboundFieldFallbackAndUnmappedButton() {
        // Field fallback
        AlertTemplateEntity tpl = new AlertTemplateEntity(UUID.randomUUID(), "tpl_field_fallback", "Title", "UTILITY", "en", "Text", null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null);
        templateRepo.persist(tpl);

        WebhookTemplateStatusDTO rawStatus = new WebhookTemplateStatusDTO("APPROVED", "meta_fb_1", null, "en", null);
        WebhookValueDTO valFb = new WebhookValueDTO("whatsapp", null, null, null, rawStatus, null, "APPROVED", "meta_fb_1", null, "en", null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("tpl_field_fallback", valFb)))))));

        assertEquals(TemplateStatus.APPROVED, tpl.getStatus());
        assertEquals("meta_fb_1", tpl.getMetaTemplateId());

        // Unmapped button reply fallback
        String phone = "+919876543210";
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, phone, false, true);
        contactRepo.persist(contact);

        WebhookInteractiveDTO interactive = new WebhookInteractiveDTO("button_reply", new WebhookButtonReplyDTO("UNKNOWN_PAYLOAD", "Unknown"), null);
        WebhookMessageDTO btnMsg = new WebhookMessageDTO(phone, "msg-unmapped", "1700000000", "interactive", null, interactive, null);
        WebhookValueDTO valUnmapped = new WebhookValueDTO("whatsapp", null, null, List.of(btnMsg), null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", valUnmapped)))))));

        assertTrue(contact.isHumanSessionActive());
    }

    @Test
    @DisplayName("processInbound processes inbound image and document media and extends human session")
    void testInboundImageAndDocumentMedia() {
        String phone = "+919876543210";
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, phone, false, true);
        contactRepo.persist(contact);

        // 1. Inbound Image
        WebhookMediaDTO imageDto = new WebhookMediaDTO("media_img_99", "image/jpeg", "sha256img", "Here is my photo", null);
        WebhookMessageDTO imgMsg = new WebhookMessageDTO(phone, "wamid.inbound.img1", "1700000000", "image", null, null, null, imageDto, null);
        WebhookValueDTO valImg = new WebhookValueDTO("whatsapp", null, null, List.of(imgMsg), null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", valImg)))))));

        Optional<AlertMessageEntity> savedImg = messageRepo.findByMetaMessageId("wamid.inbound.img1");
        assertTrue(savedImg.isPresent());
        assertEquals("IMAGE", savedImg.get().getMessageType());
        assertEquals("media_img_99", savedImg.get().getMediaId());
        assertEquals("/alerts/media/media_img_99", savedImg.get().getMediaUrl());
        assertEquals("Here is my photo", savedImg.get().getContent());
        assertTrue(contact.isHumanSessionActive());

        // 2. Inbound Document
        WebhookMediaDTO docDto = new WebhookMediaDTO("media_doc_88", "application/pdf", "sha256doc", null, "my_report.pdf");
        WebhookMessageDTO docMsg = new WebhookMessageDTO(phone, "wamid.inbound.doc1", "1700000010", "document", null, null, null, null, docDto);
        WebhookValueDTO valDoc = new WebhookValueDTO("whatsapp", null, null, List.of(docMsg), null);
        processor.processInbound(EventMessage.of(() -> "inbound", new WhatsAppWebhookPayloadDTO("object", List.of(new WebhookEntryDTO("id", List.of(new WebhookChangeDTO("messages", valDoc)))))));

        Optional<AlertMessageEntity> savedDoc = messageRepo.findByMetaMessageId("wamid.inbound.doc1");
        assertTrue(savedDoc.isPresent());
        assertEquals("DOCUMENT", savedDoc.get().getMessageType());
        assertEquals("media_doc_88", savedDoc.get().getMediaId());
        assertEquals("/alerts/media/media_doc_88", savedDoc.get().getMediaUrl());
        assertEquals("my_report.pdf", savedDoc.get().getContent());
    }

    @Test
    @DisplayName("processOutbound records outbound media fields when present")
    void testOutboundMediaDispatch() {
        String phone = "+919876543210";
        UUID userId = UUID.randomUUID();
        userReadService.validUsers.add(userId);
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, phone, false, true);
        contactRepo.persist(contact);

        dispatcherFactory.customDispatcher = new IAlertDispatcher() {
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public AlertDispatchResult dispatch(AlertDispatchRequest req) {
                return AlertDispatchResult.success("wamid.media.123", "{\"status\":\"ok\"}");
            }
        };

        UUID messageId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(
                null,
                userId,
                "Check this invoice",
                messageId,
                "/alerts/media/inv_123",
                "inv_123",
                "DOCUMENT"
        );

        EventMessage<AlertEventPayloadDTO> msg = EventMessage.of(() -> "test.queue", payload);
        processor.processOutbound(msg);

        assertEquals(1, messageRepo.messages.size());
        AlertMessageEntity recorded = messageRepo.messages.get(0);
        assertEquals("DOCUMENT", recorded.getMessageType());
        assertEquals("inv_123", recorded.getMediaId());
        assertEquals("/alerts/media/inv_123", recorded.getMediaUrl());
        assertEquals("Check this invoice", recorded.getContent());
        assertEquals(AlertMessageStatus.SENT, recorded.getStatus());
    }
}
