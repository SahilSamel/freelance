package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertErrorCode;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.template.entity.*;
import com.freelance.alerts.message.entity.*;
import com.freelance.alerts.preference.entity.*;
import com.freelance.alerts.template.repository.*;
import com.freelance.alerts.message.repository.*;
import com.freelance.alerts.preference.repository.*;
import com.freelance.event.exception.EventDelayedException;
import com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorAdapter;
import com.freelance.dispatcher.AlertDispatcherFactory;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppOutboundConsumer;
import com.freelance.user.entity.User;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.user.repository.UserRepository;
import com.freelance.user.read.UserReadService;
import com.freelance.user.read.assembler.UserReadAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AlertOutboundConsumerTest {

    static class FakeUserRepository extends UserRepository {
        final Map<UUID, User> users = new HashMap<>();

        @Override
        public User get(UUID id) {
            return users.get(id);
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
        }
    }

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<UUID, AlertTemplateEntity> templates = new HashMap<>();

        @Override
        public AlertTemplateEntity get(UUID id) {
            return templates.get(id);
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            templates.put(entity.getId(), entity);
        }
    }

    static class FakeUserContactRepository extends UserContactRepository {
        final Map<String, UserContactEntity> contacts = new HashMap<>();

        private String key(UUID userId, AlertChannel channel) {
            return userId + ":" + channel;
        }

        @Override
        public Optional<UserContactEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            return Optional.ofNullable(contacts.get(key(userId, channel)));
        }

        public void save(UserContactEntity contact) {
            contacts.put(key(contact.getUserId(), contact.getChannel()), contact);
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

    static class FakeAlertMessageRepository extends AlertMessageRepository {
        final List<AlertMessageEntity> persisted = new ArrayList<>();
        AlertMessageEntity latestMessage;

        @Override
        public void persist(AlertMessageEntity entity) {
            persisted.add(entity);
            latestMessage = entity;
        }

        @Override
        public Optional<AlertMessageEntity> findLatestByUserId(UUID userId) {
            return Optional.ofNullable(latestMessage);
        }

        @Override
        public Optional<AlertMessageEntity> findLatestByUserAndChannel(UUID userId, AlertChannel channel) {
            for (int i = persisted.size() - 1; i >= 0; i--) {
                AlertMessageEntity m = persisted.get(i);
                if (m.getUserId().equals(userId) && m.getChannel() == channel) {
                    return Optional.of(m);
                }
            }
            return Optional.empty();
        }
    }

    private FakeUserRepository userRepo;
    private UserReadService userReadService;
    private FakeAlertTemplateRepository templateRepo;
    private FakeUserContactRepository contactRepo;
    private FakeAlertChannelConfigRepository channelConfigRepo;
    private FakeAlertMessageRepository messageRepo;
    private WhatsAppDispatcherService dispatcherService;
    private AlertDispatcherFactory factory;
    private WhatsAppOutboundConsumer consumer;

    private AlertTemplateEntity sampleTemplate;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        WhatsAppErrorAdapter.resumeQueue();

        userRepo = new FakeUserRepository();
        userReadService = new UserReadService(userRepo, new UserReadAssembler());
        templateRepo = new FakeAlertTemplateRepository();
        contactRepo = new FakeUserContactRepository();
        channelConfigRepo = new FakeAlertChannelConfigRepository();
        messageRepo = new FakeAlertMessageRepository();
        dispatcherService = new WhatsAppDispatcherService(messageRepo);
        factory = new AlertDispatcherFactory(dispatcherService);

        consumer = new WhatsAppOutboundConsumer(
                userReadService,
                templateRepo,
                contactRepo,
                channelConfigRepo,
                messageRepo,
                factory
        );

        sampleTemplate = new AlertTemplateEntity(
                UUID.randomUUID(),
                "welcome_msg",
                "Welcome Notification",
                "UTILITY",
                "en",
                "Hello, welcome to our service!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                null,
                null
        );
        templateRepo.persist(sampleTemplate);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
        WhatsAppErrorAdapter.resumeQueue();
    }

    @Test
    @DisplayName("Should drop event without recording message when user does not exist in UserRepository")
    void testInvalidUserId_dropsGracefully() {
        UUID nonExistentUserId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), nonExistentUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        consumer.handle(message);

        assertEquals(0, messageRepo.persisted.size(), "Should drop event without persisting message for non-existent user");
    }

    @Test
    @DisplayName("Should successfully dispatch alert when user is valid and has contact information")
    void testValidUser_withContact_dispatchesSuccessfully() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "john_doe", "hashed_pass"));

        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890");
        contactRepo.save(contact);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity recorded = messageRepo.persisted.getFirst();
        assertEquals(validUserId, recorded.getUserId());
        assertEquals(AlertMessageStatus.SENT, recorded.getStatus());
        assertNull(recorded.getErrorCode());
        assertNotNull(recorded.getMetaMessageId());
        assertEquals(AlertDirection.OUTBOUND, recorded.getDirection());
    }

    @Test
    @DisplayName("Should record failed message with ERR_NO_CONTACT_FOUND when valid user has no contact")
    void testValidUser_noContact_recordsFailedMessage() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "alice", "hashed_pass"));

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity recorded = messageRepo.persisted.getFirst();
        assertEquals(validUserId, recorded.getUserId());
        assertEquals(AlertMessageStatus.FAILED, recorded.getStatus());
        assertEquals(AlertErrorCode.ERR_NO_CONTACT_FOUND, recorded.getErrorCode());
    }

    @Test
    @DisplayName("Should record failed message with ERR_USER_OPTED_OUT when user has opted out")
    void testValidUser_optedOut_recordsFailedMessage() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "bob", "hashed_pass"));

        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890", true, true, null);
        contactRepo.save(contact);

        AlertTemplateEntity marketingTemplate = new AlertTemplateEntity(
                UUID.randomUUID(),
                "promo_msg",
                "Promotional Offer",
                "MARKETING",
                "en",
                "Special discount just for you!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                null,
                null
        );
        templateRepo.persist(marketingTemplate);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(marketingTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity recorded = messageRepo.persisted.getFirst();
        assertEquals(validUserId, recorded.getUserId());
        assertEquals(AlertMessageStatus.FAILED, recorded.getStatus());
        assertEquals(AlertErrorCode.ERR_USER_OPTED_OUT, recorded.getErrorCode());
        assertNotNull(recorded.getRawErrorDetails());
        assertTrue(recorded.getRawErrorDetails().contains("opted out"));
    }

    @Test
    @DisplayName("Should record failed message with ERR_CHANNEL_INACTIVE when contact channel is inactive")
    void testValidUser_channelInactive_recordsFailedMessage() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "charlie", "hashed_pass"));

        UserContactEntity contact = new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890", false, false, null);
        contactRepo.save(contact);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity recorded = messageRepo.persisted.getFirst();
        assertEquals(validUserId, recorded.getUserId());
        assertEquals(AlertMessageStatus.FAILED, recorded.getStatus());
        assertEquals(AlertErrorCode.ERR_CHANNEL_INACTIVE, recorded.getErrorCode());
        assertNotNull(recorded.getRawErrorDetails());
        assertTrue(recorded.getRawErrorDetails().contains("inactive"));
    }


    @Test
    @DisplayName("Should drop event gracefully when template does not exist")
    void testMissingTemplate_dropsGracefully() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "eve", "hashed_pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890"));

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(UUID.randomUUID(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        consumer.handle(message);

        assertEquals(0, messageRepo.persisted.size());
    }

    @Test
    @DisplayName("Should drop event gracefully when payload is null or incomplete")
    void testNullOrIncompletePayload_dropsGracefully() {
        EventMessage<AlertEventPayloadDTO> nullPayloadMsg = EventMessage.of("alert.outbound.dispatch", null);
        consumer.handle(nullPayloadMsg);

        AlertEventPayloadDTO missingUserId = new AlertEventPayloadDTO(sampleTemplate.getId(), null);
        consumer.handle(EventMessage.of("alert.outbound.dispatch", missingUserId));

        AlertEventPayloadDTO missingTemplateId = new AlertEventPayloadDTO(null, UUID.randomUUID());
        consumer.handle(EventMessage.of("alert.outbound.dispatch", missingTemplateId));

        assertEquals(0, messageRepo.persisted.size());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when outbound queue is paused")
    void testQueuePaused_throwsIllegalStateException() {
        WhatsAppErrorAdapter.pauseQueue();

        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "paused_user", "pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890"));

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        assertThrows(IllegalStateException.class, () -> consumer.handle(message));
    }

    @Test
    @DisplayName("Should instantiate channel consumers with default no-arg constructors")
    void testDefaultConstructor() {
        WhatsAppOutboundConsumer waConsumer = new WhatsAppOutboundConsumer();
        assertNotNull(waConsumer);
        assertEquals(AlertChannel.WHATSAPP, waConsumer.getChannel());
    }

    @Test
    @DisplayName("Should record failed message with ERR_CHANNEL_MISMATCH when template channel does not match consumer")
    void testChannelMismatch_recordsFailedMessage() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "mismatch_user", "pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+123456789"));

        AlertTemplateEntity nonWaTemplate = new AlertTemplateEntity(
                UUID.randomUUID(),
                "non_wa_tpl",
                "Non WhatsApp Template",
                "UTILITY",
                "en",
                "Hello via non-wa",
                null,
                null,
                null,
                TemplateStatus.APPROVED,
                true,
                null,
                null
        );
        templateRepo.persist(nonWaTemplate);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(nonWaTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity recorded = messageRepo.persisted.getFirst();
        assertEquals(AlertMessageStatus.FAILED, recorded.getStatus());
        assertEquals(AlertErrorCode.ERR_CHANNEL_MISMATCH, recorded.getErrorCode());
    }

    @Test
    @DisplayName("Should record failed message with ERR_CHANNEL_NOT_AVAILABLE when no dispatcher is found")
    void testNoDispatcherAvailable_recordsFailedMessage() {
        AlertDispatcherFactory emptyFactory =
                new AlertDispatcherFactory(false);

        WhatsAppOutboundConsumer consumerWithoutDispatchers = new WhatsAppOutboundConsumer(
                userReadService,
                templateRepo,
                contactRepo,
                channelConfigRepo,
                messageRepo,
                emptyFactory
        );

        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "frank", "pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890"));

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumerWithoutDispatchers.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity recorded = messageRepo.persisted.getFirst();
        assertEquals(AlertMessageStatus.FAILED, recorded.getStatus());
        assertEquals(AlertErrorCode.ERR_CHANNEL_NOT_AVAILABLE, recorded.getErrorCode());
    }

    @Test
    @DisplayName("Should throw AlertDelayedException when user is awaiting human reply on WhatsApp")
    void testUserAwaitingHumanReply_throwsAlertDelayedException() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "awaiting_user", "pass"));

        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                validUserId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                false,
                true,
                Instant.now().plusSeconds(3600)
        );
        contactRepo.save(contact);

        // Record a previous message indicating human reply is required
        AlertMessageEntity previousMessage = new AlertMessageEntity(
                UUID.randomUUID(),
                validUserId,
                AlertChannel.WHATSAPP,
                AlertDirection.INBOUND,
                "BUTTON_REPLY",
                "I need help",
                "wamid.prev",
                AlertMessageStatus.SENT,
                null,
                null,
                Instant.now().minusSeconds(3600)
        );
        messageRepo.latestMessage = previousMessage;

        AlertTemplateEntity marketingTemplate = new AlertTemplateEntity(
                UUID.randomUUID(),
                "promo_help",
                "Promotional",
                "MARKETING",
                "en",
                "Check out these deals!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                null,
                null
        );
        templateRepo.persist(marketingTemplate);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(marketingTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        EventDelayedException ex = assertThrows(EventDelayedException.class, () -> consumer.handle(message));
        assertTrue(ex.getMessage().contains("waiting for human reply"));
        // Ensure no message was persisted as SENT or FAILED (it is delayed)
        assertEquals(0, messageRepo.persisted.size());
    }

    @Test
    @DisplayName("Should persist FAILED message with ERR_HUMAN_REPLY_TIMEOUT on dead-letter when cause is AlertDelayedException")
    void testDeadLetter_withAlertDelayedException_recordsHumanReplyTimeout() {
        UUID validUserId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumer.onDeadLetter(message, new EventDelayedException("User waited too long"));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity deadLetterRecord = messageRepo.persisted.getFirst();
        assertEquals(validUserId, deadLetterRecord.getUserId());
        assertEquals(AlertMessageStatus.FAILED, deadLetterRecord.getStatus());
        assertEquals(AlertErrorCode.ERR_HUMAN_REPLY_TIMEOUT, deadLetterRecord.getErrorCode());
        assertEquals("User waited too long", deadLetterRecord.getRawErrorDetails());
        assertEquals(sampleTemplate.getContent(), deadLetterRecord.getContent());
    }

    @Test
    @DisplayName("Should persist FAILED message with ERR_MAX_RETRIES_EXCEEDED on dead-letter for non-delay cause")
    void testDeadLetter_withOtherException_recordsMaxRetriesExceeded() {
        UUID validUserId = UUID.randomUUID();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumer.onDeadLetter(message, new RuntimeException("Fatal downstream service error"));

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity deadLetterRecord = messageRepo.persisted.getFirst();
        assertEquals(validUserId, deadLetterRecord.getUserId());
        assertEquals(AlertMessageStatus.FAILED, deadLetterRecord.getStatus());
        assertEquals(AlertErrorCode.ERR_MAX_RETRIES_EXCEEDED, deadLetterRecord.getErrorCode());
        assertEquals("Fatal downstream service error", deadLetterRecord.getRawErrorDetails());
    }

    @Test
    @DisplayName("Should successfully dispatch raw text message without template lookup")
    void testRawContent_dispatchesSuccessfully() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "staff_user", "hashed_pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890"));

        UUID messageId = UUID.randomUUID();
        String rawText = "Hello from staff support!";

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(null, validUserId, rawText, messageId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.getFirst();
        assertEquals(messageId, persisted.getId());
        assertEquals(validUserId, persisted.getUserId());
        assertEquals("TEXT", persisted.getMessageType());
        assertEquals(rawText, persisted.getContent());
        assertEquals(AlertMessageStatus.SENT, persisted.getStatus());
    }

    @Test
    @DisplayName("Should drop raw text message when user is opted out")
    void testRawContent_userOptedOut_dropsMessage() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "opted_out_user", "hashed_pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890", true, true, null));

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(null, validUserId, "Staff reply", UUID.randomUUID());
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity persisted = messageRepo.persisted.getFirst();
        assertEquals(AlertMessageStatus.FAILED, persisted.getStatus());
        assertEquals(AlertErrorCode.ERR_USER_OPTED_OUT, persisted.getErrorCode());
        assertNotNull(persisted.getRawErrorDetails());
    }

    @Test
    @DisplayName("Should record failed message with ERR_CHANNEL_PAUSED and route to dead letter when channel is down")
    void testGlobalChannelDown_recordsFailedAndDeadLetters() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "alice", "hashed_pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890"));
        channelConfigRepo.storage.put(AlertChannel.WHATSAPP, new com.freelance.alerts.channel.entity.AlertChannelConfigEntity(
                AlertChannel.WHATSAPP, false, "Meta API maintenance"
        ));

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(sampleTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.dispatch", payload);

        consumer.handle(message);

        assertEquals(2, messageRepo.persisted.size(), "Should record failed alert and dead letter entry");
        AlertMessageEntity failedAlert = messageRepo.persisted.getFirst();
        assertEquals(AlertMessageStatus.FAILED, failedAlert.getStatus());
        assertEquals(AlertErrorCode.ERR_CHANNEL_PAUSED, failedAlert.getErrorCode());
        assertTrue(failedAlert.getRawErrorDetails().contains("Meta API maintenance"));
    }

    @Test
    @DisplayName("Should fail outbound dispatch with ERR_SESSION_WINDOW_CLOSED when interactive list template has > 3 buttons and session is closed")
    void testInteractiveList_sessionClosed_failsWithSessionWindowClosed() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "carol", "hashed_pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890"));

        AlertTemplateEntity listTemplate = new AlertTemplateEntity(
                UUID.randomUUID(),
                "menu_options",
                "Menu Options",
                "UTILITY",
                "en",
                "Please select an option from the menu:",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );
        for (int i = 1; i <= 4; i++) {
            listTemplate.getButtons().add(new AlertButtonEntity(
                    UUID.randomUUID(), listTemplate, "Option " + i, ButtonType.QUICK_REPLY, "OPT_" + i, i
            ));
        }
        templateRepo.persist(listTemplate);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(listTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumer.handle(message);

        assertEquals(1, messageRepo.persisted.size());
        AlertMessageEntity failedAlert = messageRepo.persisted.getFirst();
        assertEquals(AlertMessageStatus.FAILED, failedAlert.getStatus());
        assertEquals(AlertErrorCode.ERR_SESSION_WINDOW_CLOSED, failedAlert.getErrorCode());
        assertTrue(failedAlert.getRawErrorDetails().contains("requires an active 24-hour customer session"));
    }

    @Test
    @DisplayName("Should successfully dispatch interactive list template when 24h interactive session is active")
    void testInteractiveList_sessionActive_dispatchesSuccessfully() {
        UUID validUserId = UUID.randomUUID();
        userRepo.users.put(validUserId, new User(validUserId, "david", "hashed_pass"));
        contactRepo.save(new UserContactEntity(UUID.randomUUID(), validUserId, AlertChannel.WHATSAPP, "+1234567890"));

        // Simulate an inbound message within the 24-hour window
        AlertMessageEntity priorInbound = new AlertMessageEntity(
                UUID.randomUUID(),
                validUserId,
                AlertChannel.WHATSAPP,
                AlertDirection.INBOUND,
                "TEXT",
                "Hello, I need help",
                "wamid.prior123",
                AlertMessageStatus.DELIVERED,
                null,
                null,
                Instant.now().minusSeconds(1800)
        );
        messageRepo.persist(priorInbound);

        AlertTemplateEntity listTemplate = new AlertTemplateEntity(
                UUID.randomUUID(),
                "menu_options_active",
                "Menu Options Active",
                "UTILITY",
                "en",
                "Please select an option:",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                null,
                null
        );
        for (int i = 1; i <= 5; i++) {
            listTemplate.getButtons().add(new AlertButtonEntity(
                    UUID.randomUUID(), listTemplate, "Choice " + i, ButtonType.QUICK_REPLY, "CHOICE_" + i, i
            ));
        }
        templateRepo.persist(listTemplate);

        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(listTemplate.getId(), validUserId);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of("alert.outbound.whatsapp", payload);

        consumer.handle(message);

        assertEquals(2, messageRepo.persisted.size(), "Should have prior inbound message + dispatched outbound list alert");
        AlertMessageEntity outboundAlert = messageRepo.persisted.get(1);
        assertEquals(AlertMessageStatus.SENT, outboundAlert.getStatus());
        assertNull(outboundAlert.getErrorCode());
        assertNotNull(outboundAlert.getMetaMessageId());
    }
}