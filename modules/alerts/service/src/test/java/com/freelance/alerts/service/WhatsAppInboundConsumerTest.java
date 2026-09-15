package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.AlertsEngine;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.webhook.*;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonReplyRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.templates.InboundWelcomeTemplate;
import com.freelance.consumer.alerts.inbound.whatsapp.WhatsAppInboundConsumer;
import com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorAdapter;
import com.freelance.event.model.EventMessage;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppInboundConsumerTest {

    static class FakeAlertMessageRepository extends AlertMessageRepository {
        final Map<String, AlertMessageEntity> byMetaId = new HashMap<>();
        final List<AlertMessageEntity> persisted = new ArrayList<>();

        @Override
        public Optional<AlertMessageEntity> findByMetaMessageId(String metaMessageId) {
            if (metaMessageId == null || metaMessageId.isBlank()) return Optional.empty();
            return Optional.ofNullable(byMetaId.get(metaMessageId));
        }

        @Override
        public void persist(AlertMessageEntity entity) {
            persisted.add(entity);
            if (entity.getMetaMessageId() != null) {
                byMetaId.put(entity.getMetaMessageId(), entity);
            }
        }

        @Override
        public AlertMessageEntity merge(AlertMessageEntity entity) {
            if (entity.getMetaMessageId() != null) {
                byMetaId.put(entity.getMetaMessageId(), entity);
            }
            return entity;
        }
    }

    static class FakeUserContactRepository extends UserContactRepository {
        final Map<String, UserContactEntity> contactsByPhone = new HashMap<>();
        final Map<String, UserContactEntity> contactsByUser = new HashMap<>();

        private String phoneKey(String contact, AlertChannel channel) {
            return contact + ":" + channel;
        }

        private String userKey(UUID userId, AlertChannel channel) {
            return userId + ":" + channel;
        }

        @Override
        public Optional<UserContactEntity> findByContactAndChannel(String contact, AlertChannel channel) {
            return Optional.ofNullable(contactsByPhone.get(phoneKey(contact, channel)));
        }

        @Override
        public Optional<UserContactEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            return Optional.ofNullable(contactsByUser.get(userKey(userId, channel)));
        }

        @Override
        public void persist(UserContactEntity entity) {
            contactsByPhone.put(phoneKey(entity.getContactValue(), entity.getChannel()), entity);
            contactsByUser.put(userKey(entity.getUserId(), entity.getChannel()), entity);
        }

        @Override
        public UserContactEntity merge(UserContactEntity entity) {
            persist(entity);
            return entity;
        }
    }

    static class FakeAlertChannelConfigRepository extends com.freelance.alerts.channel.repository.AlertChannelConfigRepository {
        final Map<AlertChannel, com.freelance.alerts.channel.entity.AlertChannelConfigEntity> storage = new HashMap<>();

        @Override
        public Optional<com.freelance.alerts.channel.entity.AlertChannelConfigEntity> findByChannel(AlertChannel channel) {
            return Optional.ofNullable(storage.get(channel));
        }

        @Override
        public boolean isChannelAvailable(AlertChannel channel) {
            com.freelance.alerts.channel.entity.AlertChannelConfigEntity config = storage.get(channel);
            if (config == null) return true;
            return config.isActive();
        }
    }

    static class FakeAlertButtonReplyRepository extends AlertButtonReplyRepository {
        final Map<String, AlertButtonReplyEntity> replies = new HashMap<>();

        @Override
        public Optional<AlertButtonReplyEntity> findByButtonPayload(String payload) {
            if (payload == null || payload.isBlank()) return Optional.empty();
            return Optional.ofNullable(replies.get(payload));
        }

        public void save(String payload, AlertButtonReplyEntity entity) {
            replies.put(payload, entity);
        }
    }

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<UUID, AlertTemplateEntity> byId = new HashMap<>();
        final Map<String, AlertTemplateEntity> byKey = new HashMap<>();

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
        public void persist(AlertTemplateEntity entity) {
            byId.put(entity.getId(), entity);
            byKey.put(entity.getTemplateKey(), entity);
        }

        @Override
        public AlertTemplateEntity merge(AlertTemplateEntity entity) {
            persist(entity);
            return entity;
        }
    }

    private TransactionContext context;
    private FakeAlertMessageRepository messageRepo;
    private FakeUserContactRepository contactRepo;
    private FakeAlertChannelConfigRepository channelConfigRepo;
    private FakeAlertButtonReplyRepository buttonReplyRepo;
    private FakeAlertTemplateRepository templateRepo;
    private AlertsEngine alertsEngine;
    private WhatsAppInboundConsumer consumer;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        WhatsAppErrorAdapter.resumeQueue();

        messageRepo = new FakeAlertMessageRepository();
        contactRepo = new FakeUserContactRepository();
        channelConfigRepo = new FakeAlertChannelConfigRepository();
        buttonReplyRepo = new FakeAlertButtonReplyRepository();
        templateRepo = new FakeAlertTemplateRepository();
        alertsEngine = new AlertsEngine(templateRepo);

        consumer = new WhatsAppInboundConsumer(
                messageRepo,
                contactRepo,
                channelConfigRepo,
                buttonReplyRepo,
                templateRepo,
                alertsEngine
        );
    }

    @AfterEach
    void tearDown() {
        WhatsAppErrorAdapter.resumeQueue();
        TestContextHelper.tearDown();
    }

    private WhatsAppWebhookPayloadDTO wrapMessage(WebhookMessageDTO message) {
        WebhookValueDTO value = new WebhookValueDTO("whatsapp_business_account", null, null, List.of(message), null);
        WebhookChangeDTO change = new WebhookChangeDTO("messages", value);
        WebhookEntryDTO entry = new WebhookEntryDTO("entry_1", List.of(change));
        return new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entry));
    }

    private WhatsAppWebhookPayloadDTO wrapStatus(WebhookStatusDTO status) {
        WebhookValueDTO value = new WebhookValueDTO("whatsapp_business_account", null, List.of(status), null, null);
        WebhookChangeDTO change = new WebhookChangeDTO("messages", value);
        WebhookEntryDTO entry = new WebhookEntryDTO("entry_1", List.of(change));
        return new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entry));
    }

    // =========================================================================
    // INBOUND MESSAGES
    // =========================================================================

    @Test
    @DisplayName("Should persist inbound text message and auto-provision unknown contact")
    void testInboundTextMessage_unknownContact() {
        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("Hello Support");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_inbound_1", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals(AlertDirection.INBOUND, persisted.getDirection());
        assertEquals(AlertChannel.WHATSAPP, persisted.getChannel());
        assertEquals("TEXT", persisted.getMessageType());
        assertEquals("Hello Support", persisted.getContent());
        assertEquals("wamid_inbound_1", persisted.getMetaMessageId());
        assertEquals(AlertMessageStatus.DELIVERED, persisted.getStatus());
        assertEquals(Instant.ofEpochSecond(1700000000L), persisted.getTimestamp());

        // Verify contact auto-provisioned
        Optional<UserContactEntity> contactOpt = contactRepo.findByContactAndChannel("+919876543210", AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertEquals(persisted.getUserId(), contactOpt.get().getUserId());
        assertNull(contactOpt.get().getHumanSessionExpiresAt(), "Human session should not start on freeform text");
    }

    @Test
    @DisplayName("Should map inbound message to existing user contact without duplicating")
    void testInboundTextMessage_existingContact() {
        UUID existingUserId = UUID.randomUUID();
        UserContactEntity existingContact = new UserContactEntity(
                UUID.randomUUID(), existingUserId, AlertChannel.WHATSAPP, "+919876543210"
        );
        contactRepo.persist(existingContact);

        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("Existing User Query");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_inbound_2", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals(existingUserId, persisted.getUserId());
        assertEquals(1, contactRepo.contactsByUser.size());
    }

    @Test
    @DisplayName("Should skip duplicate inbound message with already-processed wamid (idempotency)")
    void testInboundTextMessage_idempotency() {
        AlertMessageEntity existing = new AlertMessageEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.INBOUND,
                "TEXT", "Previous message", "wamid_duplicate_test", AlertMessageStatus.DELIVERED,
                null, null, Instant.now()
        );
        messageRepo.persist(existing);

        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("Duplicate retry body");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_duplicate_test", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        // No additional message should be persisted
        assertEquals(1, messageRepo.persisted.size());
    }

    @Test
    @DisplayName("Should mark user opted-out and requireHuman=false when inbound text is STOP")
    void testInboundTextMessage_optOut() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210"
        );
        contactRepo.persist(contact);

        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("STOP");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_optout_1", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("STOP", persisted.getContent());

        Optional<UserContactEntity> contactOpt = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertTrue(contactOpt.get().isOptedOut());
        assertFalse(contactOpt.get().isActive());
    }

    @Test
    @DisplayName("Should mark user opted-out when inbound text is 'opt out' or 'STOP.'")
    void testInboundTextMessage_optOutPhrases() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "919876543210"
        );
        contactRepo.persist(contact);

        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("opt out");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "919876543210", "wamid_optout_phrase", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        Optional<UserContactEntity> contactOpt = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertTrue(contactOpt.get().isOptedOut());
        assertFalse(contactOpt.get().isActive());
    }

    @Test
    @DisplayName("Should mark user opted-in and requireHuman=false when inbound text is START")
    void testInboundTextMessage_optIn() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210", true, false, null
        );
        contactRepo.persist(contact);

        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("START");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_optin_1", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("START", persisted.getContent());

        Optional<UserContactEntity> contactOpt = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertFalse(contactOpt.get().isOptedOut());
        assertTrue(contactOpt.get().isActive());
    }

    @Test
    @DisplayName("Should mark user opted-in when inbound text is 'SUBSCRIBE' or 'YES'")
    void testInboundTextMessage_optInPhrases() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "919876543210", true, false, null
        );
        contactRepo.persist(contact);

        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("SUBSCRIBE");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "919876543210", "wamid_optin_sub", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        Optional<UserContactEntity> contactOpt = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertFalse(contactOpt.get().isOptedOut());
        assertTrue(contactOpt.get().isActive());
    }

    @Test
    @DisplayName("Should handle button reply with automated response template and emit follow-up alert")
    void testInboundButtonReply_withReplyTemplate() {
        UUID replyTemplateId = UUID.randomUUID();
        AlertTemplateEntity replyTemplate = new AlertTemplateEntity(
                replyTemplateId, "TPL_OPT_IN_ACK", "Opt-in Ack", "UTILITY", "en",
                "Thank you for opting in!", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED, true, null, null
        );
        templateRepo.persist(replyTemplate);

        AlertButtonEntity button = new AlertButtonEntity(UUID.randomUUID(), replyTemplate, "Opt In", ButtonType.QUICK_REPLY, "PAYLOAD_OPT_IN", 1);
        AlertButtonReplyEntity buttonReply = new AlertButtonReplyEntity(UUID.randomUUID(), button, replyTemplate, false);
        buttonReplyRepo.save("PAYLOAD_OPT_IN", buttonReply);

        WebhookInteractiveDTO interactiveDTO = new WebhookInteractiveDTO(
                "button_reply", new WebhookButtonReplyDTO("PAYLOAD_OPT_IN", "Opt In")
        );
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_button_1", "1700000000", "interactive", null, interactiveDTO, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("BUTTON_REPLY", persisted.getMessageType());
        assertEquals("Opt In", persisted.getContent());

        Optional<UserContactEntity> contactOpt1 = contactRepo.findByContactAndChannel("+919876543210", AlertChannel.WHATSAPP);
        assertTrue(contactOpt1.isPresent());
        assertFalse(contactOpt1.get().isHumanSessionActive());
    }

    @Test
    @DisplayName("Should handle terminal button reply (requiresHuman=true, no reply template)")
    void testInboundButtonReply_terminalHumanAgent() {
        AlertButtonEntity button = new AlertButtonEntity(UUID.randomUUID(), null, "Talk to Human", ButtonType.QUICK_REPLY, "PAYLOAD_TALK_HUMAN", 1);
        AlertButtonReplyEntity buttonReply = new AlertButtonReplyEntity(UUID.randomUUID(), button, null, true);
        buttonReplyRepo.save("PAYLOAD_TALK_HUMAN", buttonReply);

        WebhookInteractiveDTO interactiveDTO = new WebhookInteractiveDTO(
                "button_reply", new WebhookButtonReplyDTO("PAYLOAD_TALK_HUMAN", "Talk to Human")
        );
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_button_2", "1700000000", "interactive", null, interactiveDTO, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("BUTTON_REPLY", persisted.getMessageType());
        assertEquals("Talk to Human", persisted.getContent());

        Optional<UserContactEntity> contactOpt = contactRepo.findByContactAndChannel("+919876543210", AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertTrue(contactOpt.get().isHumanSessionActive(), "Should flag human intervention needed");
    }

    @Test
    @DisplayName("Should handle interactive list reply with automated response template and emit follow-up alert")
    void testInboundListReply_withReplyTemplate() {
        UUID replyTemplateId = UUID.randomUUID();
        AlertTemplateEntity replyTemplate = new AlertTemplateEntity(
                replyTemplateId, "TPL_FAQ_ANSWER", "FAQ Answer", "UTILITY", "en",
                "Here is your answer", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED, true, null, null
        );
        templateRepo.persist(replyTemplate);

        AlertButtonEntity button = new AlertButtonEntity(UUID.randomUUID(), replyTemplate, "Check FAQ", ButtonType.QUICK_REPLY, "PAYLOAD_LIST_FAQ", 1);
        AlertButtonReplyEntity buttonReply = new AlertButtonReplyEntity(UUID.randomUUID(), button, replyTemplate, false);
        buttonReplyRepo.save("PAYLOAD_LIST_FAQ", buttonReply);

        WebhookInteractiveDTO interactiveDTO = new WebhookInteractiveDTO(
                "list_reply", null, new WebhookListReplyDTO("PAYLOAD_LIST_FAQ", "Check FAQ", "Read our FAQs")
        );
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_list_1", "1700000000", "interactive", null, interactiveDTO, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("BUTTON_REPLY", persisted.getMessageType());
        assertEquals("Check FAQ", persisted.getContent());

        Optional<UserContactEntity> contactOpt = contactRepo.findByContactAndChannel("+919876543210", AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertFalse(contactOpt.get().isHumanSessionActive());
    }

    @Test
    @DisplayName("Should handle interactive list reply for terminal human agent action")
    void testInboundListReply_terminalHumanAgent() {
        AlertButtonEntity button = new AlertButtonEntity(UUID.randomUUID(), null, "Speak to Specialist", ButtonType.QUICK_REPLY, "PAYLOAD_LIST_HUMAN", 1);
        AlertButtonReplyEntity buttonReply = new AlertButtonReplyEntity(UUID.randomUUID(), button, null, true);
        buttonReplyRepo.save("PAYLOAD_LIST_HUMAN", buttonReply);

        WebhookInteractiveDTO interactiveDTO = new WebhookInteractiveDTO(
                "list_reply", null, new WebhookListReplyDTO("PAYLOAD_LIST_HUMAN", "Speak to Specialist", "Live agent triage")
        );
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_list_2", "1700000000", "interactive", null, interactiveDTO, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("BUTTON_REPLY", persisted.getMessageType());
        assertEquals("Speak to Specialist", persisted.getContent());

        Optional<UserContactEntity> contactOpt = contactRepo.findByContactAndChannel("+919876543210", AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertTrue(contactOpt.get().isHumanSessionActive(), "List selection should flag human intervention needed");
    }

    @Test
    @DisplayName("Should handle button reply with unconfigured payload safely as BUTTON_REPLY with human required")
    void testInboundButtonReply_unconfiguredPayload() {
        WebhookButtonDTO buttonDTO = new WebhookButtonDTO("Custom Option", "CUSTOM_PAYLOAD_UNCONFIGURED");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_button_3", "1700000000", "button", null, null, buttonDTO
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("BUTTON_REPLY", persisted.getMessageType());
        assertEquals("Custom Option", persisted.getContent());

        Optional<UserContactEntity> contactOpt = contactRepo.findByContactAndChannel("+919876543210", AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertTrue(contactOpt.get().isHumanSessionActive());
    }

    @Test
    @DisplayName("Should fallback to type label when message body is missing (e.g. media message)")
    void testInboundMessage_missingContentFallback() {
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_media_1", "1700000000", "image", null, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("[image]", persisted.getContent());
    }

    @Test
    @DisplayName("Should skip message safely when sender phone number is missing")
    void testInboundMessage_missingSenderPhone() {
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                null, "wamid_nophone", "1700000000", "text", new WebhookTextMessageDTO("Hi"), null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(0, messageRepo.persisted.size());
    }

    // =========================================================================
    // STATUS UPDATES
    // =========================================================================

    @Test
    @DisplayName("Should transition message status from PENDING to SENT")
    void testStatusUpdate_sent() {
        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "OTP", "wamid_status_1", AlertMessageStatus.PENDING,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        WebhookStatusDTO statusDTO = new WebhookStatusDTO("wamid_status_1", "sent", "1700000001", "919876543210", null);
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(statusDTO)));

        assertEquals(AlertMessageStatus.SENT, msg.getStatus());
    }

    @Test
    @DisplayName("Should transition message status to DELIVERED")
    void testStatusUpdate_delivered() {
        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "OTP", "wamid_status_2", AlertMessageStatus.SENT,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        WebhookStatusDTO statusDTO = new WebhookStatusDTO("wamid_status_2", "delivered", "1700000002", "919876543210", null);
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(statusDTO)));

        assertEquals(AlertMessageStatus.DELIVERED, msg.getStatus());
    }

    @Test
    @DisplayName("Should transition message status to READ")
    void testStatusUpdate_read() {
        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "OTP", "wamid_status_3", AlertMessageStatus.DELIVERED,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        WebhookStatusDTO statusDTO = new WebhookStatusDTO("wamid_status_3", "read", "1700000003", "919876543210", null);
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(statusDTO)));

        assertEquals(AlertMessageStatus.READ, msg.getStatus());
    }

    @Test
    @DisplayName("Should not regress status from READ back to DELIVERED or SENT upon delayed webhook receipts")
    void testStatusUpdate_noRegressionFromRead() {
        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "OTP", "wamid_status_4", AlertMessageStatus.READ,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        // Delayed 'delivered'
        WebhookStatusDTO delayedDelivered = new WebhookStatusDTO("wamid_status_4", "delivered", "1700000004", "919876543210", null);
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(delayedDelivered)));
        assertEquals(AlertMessageStatus.READ, msg.getStatus());

        // Delayed 'sent'
        WebhookStatusDTO delayedSent = new WebhookStatusDTO("wamid_status_4", "sent", "1700000005", "919876543210", null);
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(delayedSent)));
        assertEquals(AlertMessageStatus.READ, msg.getStatus());
    }

    @Test
    @DisplayName("Should update message status to FAILED when status is failed without error details")
    void testStatusUpdate_failedWithoutErrors() {
        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "OTP", "wamid_status_5", AlertMessageStatus.SENT,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        WebhookStatusDTO statusDTO = new WebhookStatusDTO("wamid_status_5", "failed", "1700000006", "919876543210", null);
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(statusDTO)));

        assertEquals(AlertMessageStatus.FAILED, msg.getStatus());
    }

    @Test
    @DisplayName("Should apply failure action and mark opted-out when user block error occurs")
    void testStatusUpdate_withOptOutErrorAction() {
        UUID userId = UUID.randomUUID();
        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "OTP", "wamid_status_optout", AlertMessageStatus.SENT,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        // Meta error 131049 = User blocked business
        WebhookErrorDTO errorDTO = new WebhookErrorDTO(131049, "Blocked", "User has blocked the business", null, null);
        WebhookStatusDTO statusDTO = new WebhookStatusDTO("wamid_status_optout", "failed", "1700000007", "919876543210", List.of(errorDTO));

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(statusDTO)));

        assertEquals(AlertMessageStatus.FAILED, msg.getStatus());
        assertEquals("ERR_USER_BLOCKED_BUSINESS", msg.getErrorCode());
        assertEquals("User has blocked the business", msg.getRawErrorDetails());
    }

    @Test
    @DisplayName("Should pause queue and mark message failed on critical account locked error")
    void testStatusUpdate_withCriticalAccountLocked() {
        UUID userId = UUID.randomUUID();
        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "OTP", "wamid_status_critical", AlertMessageStatus.SENT,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        // Meta error 131031 = Account locked
        WebhookErrorDTO errorDTO = new WebhookErrorDTO(131031, "Locked", "Account restricted", null, null);
        WebhookStatusDTO statusDTO = new WebhookStatusDTO("wamid_status_critical", "failed", "1700000008", "919876543210", List.of(errorDTO));

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(statusDTO)));

        assertEquals(AlertMessageStatus.FAILED, msg.getStatus());
        assertEquals("ERR_BUSINESS_ACCOUNT_LOCKED", msg.getErrorCode());
        assertTrue(WhatsAppErrorAdapter.isQueuePaused(), "Outbound queue should be paused on critical error");
    }

    @Test
    @DisplayName("Should handle status receipt for unknown wamid gracefully without throwing")
    void testStatusUpdate_unknownWamid() {
        WebhookStatusDTO statusDTO = new WebhookStatusDTO("wamid_nonexistent", "delivered", "1700000008", "919876543210", null);
        assertDoesNotThrow(() -> consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapStatus(statusDTO))));
    }

    private WhatsAppWebhookPayloadDTO wrapUserPreference(WebhookUserPreferenceDTO userPref) {
        WebhookValueDTO value = new WebhookValueDTO("whatsapp_business_account", null, null, null, null, List.of(userPref), null, null, null, null, null);
        WebhookChangeDTO change = new WebhookChangeDTO("user_preferences", value);
        WebhookEntryDTO entry = new WebhookEntryDTO("entry_1", List.of(change));
        return new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entry));
    }

    @Test
    @DisplayName("Should update user contact to opted-out on Meta user_preferences webhook with OPT_OUT")
    void testUserPreferences_optOut() {
        String phone = "+919876543210";
        UUID userId = UUID.randomUUID();
        contactRepo.persist(new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, phone, false, true, null));

        WebhookUserPreferenceDTO prefDTO = new WebhookUserPreferenceDTO(phone, null, "MARKETING", "OPT_OUT", null, null, "1700000000");
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapUserPreference(prefDTO)));

        Optional<UserContactEntity> contactOpt = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertTrue(contactOpt.get().isOptedOut());
    }

    @Test
    @DisplayName("Should auto-provision contact and set opted-out on Meta user_preferences for unknown user")
    void testUserPreferences_unknownUser_autoProvisionsAndOptsOut() {
        String phone = "+15551234567";
        WebhookUserPreferenceDTO prefDTO = new WebhookUserPreferenceDTO(phone, null, "MARKETING", "STOP_PROMOTIONS", null, null, "1700000000");
        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapUserPreference(prefDTO)));

        Optional<UserContactEntity> contactOpt = contactRepo.findByContactAndChannel(phone, AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertTrue(contactOpt.get().isOptedOut());
    }

    @Test
    @DisplayName("Should emit welcome template and not start human session on inbound freeform text")
    void testInboundTextMessage_emitsWelcomeTemplateWithoutStartingHumanSession() {
        // Seed InboundWelcomeTemplate
        AlertTemplateEntity welcomeEntity = new AlertTemplateEntity(
                UUID.randomUUID(),
                InboundWelcomeTemplate.TEMPLATE_KEY,
                "Inbound Welcome Menu",
                "UTILITY",
                "en_US",
                "Hello! Please choose:",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                null,
                null
        );
        templateRepo.persist(welcomeEntity);

        WebhookTextMessageDTO textDTO = new WebhookTextMessageDTO("What are your business hours?");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                "+919876543210", "wamid_inbound_welcome", "1700000000", "text", textDTO, null, null
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertEquals("TEXT", persisted.getMessageType());

        Optional<UserContactEntity> contactOpt = contactRepo.findByContactAndChannel("+919876543210", AlertChannel.WHATSAPP);
        assertTrue(contactOpt.isPresent());
        assertNull(contactOpt.get().getHumanSessionExpiresAt(), "Human session should NOT start on freeform text");
    }

    @Test
    @DisplayName("Should not start human session when user selects self-service button")
    void testInboundButtonReply_selfServiceOption_doesNotStartHumanSession() {
        String phone = "+919876543210";
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, phone, false, true, null);
        contactRepo.persist(contact);

        AlertButtonEntity button = new AlertButtonEntity(UUID.randomUUID(), null, "View FAQs", ButtonType.QUICK_REPLY, "TRIAGE_FAQ", 1);
        AlertButtonReplyEntity buttonReply = new AlertButtonReplyEntity(UUID.randomUUID(), button, null, false);
        buttonReplyRepo.save("TRIAGE_FAQ", buttonReply);

        WebhookButtonDTO buttonDTO = new WebhookButtonDTO("View FAQs", "TRIAGE_FAQ");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                phone, "wamid_btn_faq", "1700000000", "button", null, null, buttonDTO
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);
        assertNull(contact.getHumanSessionExpiresAt(), "Self-service click must not activate human session");
    }

    @Test
    @DisplayName("Should activate 24h human support session when user selects Need Human Support button")
    void testInboundButtonReply_humanSupportOption_startsHumanSession() {
        String phone = "+919876543210";
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, phone, false, true, null);
        contactRepo.persist(contact);

        AlertButtonEntity button = new AlertButtonEntity(UUID.randomUUID(), null, "Need Human Support", ButtonType.QUICK_REPLY, "TRIAGE_HUMAN", 3);
        AlertButtonReplyEntity buttonReply = new AlertButtonReplyEntity(UUID.randomUUID(), button, null, true);
        buttonReplyRepo.save("TRIAGE_HUMAN", buttonReply);

        WebhookButtonDTO buttonDTO = new WebhookButtonDTO("Need Human Support", "TRIAGE_HUMAN");
        WebhookMessageDTO msgDTO = new WebhookMessageDTO(
                phone, "wamid_btn_human", "1700000000", "button", null, null, buttonDTO
        );

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", wrapMessage(msgDTO)));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.get(0);

        Optional<UserContactEntity> updatedContactOpt = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        assertTrue(updatedContactOpt.isPresent());
        assertNotNull(updatedContactOpt.get().getHumanSessionExpiresAt(), "Human session timer must be active");
        assertTrue(updatedContactOpt.get().isHumanSessionActive(), "Human session must be active");
    }
}

