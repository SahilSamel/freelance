package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.AdminChatReplyRequestDTO;
import com.freelance.alerts.dto.ChatMessageDTO;
import com.freelance.alerts.dto.ChatTimelineRequestDTO;
import com.freelance.alerts.dto.ChatTimelineResponseDTO;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.chat.ChatTimelineService;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ChatTimelineServiceTest {

    private FakeAlertMessageRepository messageRepo;
    private FakeUserContactRepository contactRepo;
    private ChatTimelineService timelineService;

    @BeforeEach
    void setUp() {
        com.freelance.fixtures.TestContextHelper.setUp();
        messageRepo = new FakeAlertMessageRepository();
        contactRepo = new FakeUserContactRepository();
        timelineService = new ChatTimelineService(messageRepo, contactRepo, new WhatsAppDispatcherService());
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        com.freelance.fixtures.TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should return isHumanRequired=true when user contact has an active human session")
    void testGetTimeline_activeHumanSession() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210", false, true, Instant.now().plusSeconds(3600)
        );
        contactRepo.persist(contact);

        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.INBOUND,
                "TEXT", "I need help with my account", "wamid_1", AlertMessageStatus.DELIVERED,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        ChatTimelineResponseDTO timeline = timelineService.getTimeline(new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, 50));

        assertNotNull(timeline);
        assertEquals(userId, timeline.userId());
        assertTrue(timeline.isHumanRequired(), "Should reflect active human session from contact table");
        assertEquals(1, timeline.messages().size());
        ChatMessageDTO dto = timeline.messages().get(0);
        assertEquals(1, dto.sequenceNumber(), "Sequence number should start at 1");
        assertEquals("I need help with my account", dto.content());
        assertEquals("TEXT", dto.messageType());
    }

    @Test
    @DisplayName("Should return isHumanRequired=false when user contact human session has expired or is absent")
    void testGetTimeline_inactiveHumanSession() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210", false, true, Instant.now().minusSeconds(3600)
        );
        contactRepo.persist(contact);

        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.INBOUND,
                "TEXT", "Just checking in", "wamid_2", AlertMessageStatus.DELIVERED,
                null, null, Instant.now()
        );
        messageRepo.persist(msg);

        ChatTimelineResponseDTO timeline = timelineService.getTimeline(new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, 50));

        assertNotNull(timeline);
        assertFalse(timeline.isHumanRequired(), "Expired human session should evaluate to false");
        assertEquals(1, timeline.messages().size());
    }

    @Test
    @DisplayName("Should assign sequential sequenceNumber (1, 2, 3...) and respect limit")
    void testGetTimeline_sequentialNumbersAndLimit() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            AlertMessageEntity msg = new AlertMessageEntity(
                    UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.INBOUND,
                    "TEXT", "Message #" + (i + 1), "wamid_" + i, AlertMessageStatus.DELIVERED,
                    null, null, Instant.now().plusSeconds(i * 10)
            );
            messageRepo.persist(msg);
        }

        ChatTimelineResponseDTO timeline = timelineService.getTimeline(new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, 3));
        assertNotNull(timeline);
        assertEquals(3, timeline.messages().size());
        assertEquals(1, timeline.messages().get(0).sequenceNumber());
        assertEquals("Message #1", timeline.messages().get(0).content());
        assertEquals(2, timeline.messages().get(1).sequenceNumber());
        assertEquals("Message #2", timeline.messages().get(1).content());
        assertEquals(3, timeline.messages().get(2).sequenceNumber());
        assertEquals("Message #3", timeline.messages().get(2).content());
    }

    @Test
    @DisplayName("Should clear human support session on user contact when staff sends a reply")
    void testSendStaffReply_clearsHumanSession() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210", false, true, Instant.now().plusSeconds(3600)
        );
        contactRepo.persist(contact);
        assertTrue(contact.isHumanSessionActive(), "Precondition: human session is active");

        AdminChatReplyRequestDTO replyRequest = new AdminChatReplyRequestDTO(userId, AlertChannel.WHATSAPP, "Hello, how can I help you?");
        ChatMessageDTO sent = timelineService.sendStaffReply(replyRequest);

        assertNotNull(sent);
        assertEquals("Hello, how can I help you?", sent.content());
        assertEquals(AlertMessageStatus.PENDING, sent.status());

        // Verify that contact's human session was cleared
        Optional<UserContactEntity> updatedContact = contactRepo.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
        assertTrue(updatedContact.isPresent());
        assertNull(updatedContact.get().getHumanSessionExpiresAt(), "Staff reply should clear humanSessionExpiresAt");
        assertFalse(updatedContact.get().isHumanSessionActive(), "Human session should no longer be active");
    }

    @Test
    @DisplayName("Should include template buttons when outbound message is associated with a template")
    void testGetTimeline_includesTemplateButtons() {
        UUID userId = UUID.randomUUID();
        AlertTemplateEntity template = new AlertTemplateEntity(
                UUID.randomUUID(),
                "inbound_welcome_menu",
                "Inbound Welcome Menu",
                "UTILITY",
                "en_US",
                "Hello! How can we assist you today?",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                null,
                null
        );
        AlertButtonEntity button = new AlertButtonEntity(
                UUID.randomUUID(),
                template,
                "Need Human Support",
                ButtonType.QUICK_REPLY,
                "WELCOME_HUMAN",
                1
        );
        AlertButtonReplyEntity reply = new AlertButtonReplyEntity(
                UUID.randomUUID(),
                button,
                null,
                true
        );
        button.setReply(reply);
        template.getButtons().add(button);

        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                AlertDirection.OUTBOUND,
                "TEMPLATE",
                template.getContent(),
                template,
                "wamid_tpl_1",
                AlertMessageStatus.SENT,
                null,
                null,
                Instant.now()
        );
        messageRepo.persist(msg);

        ChatTimelineResponseDTO timeline = timelineService.getTimeline(new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, 50));
        assertNotNull(timeline);
        assertEquals(1, timeline.messages().size());
        ChatMessageDTO dto = timeline.messages().get(0);
        assertEquals("TEMPLATE", dto.messageType());
        assertNotNull(dto.buttons(), "Buttons should be present on template message");
        assertEquals(1, dto.buttons().size());
        assertEquals("Need Human Support", dto.buttons().get(0).text());
        assertEquals(ButtonType.QUICK_REPLY, dto.buttons().get(0).type());
        assertEquals("WELCOME_HUMAN", dto.buttons().get(0).payload());
        assertTrue(dto.buttons().get(0).requiresHuman());
        assertEquals(1, dto.buttons().get(0).sequenceOrder());
    }

    @Test
    @DisplayName("Should populate replyKey when button reply has an associated reply template")
    void testGetTimeline_withReplyTemplateKey() {
        UUID userId = UUID.randomUUID();
        AlertTemplateEntity targetTpl = new AlertTemplateEntity(
                UUID.randomUUID(), "menu_help_reply", "Help Reply", "UTILITY", "en_US", "How can we help?", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
        );
        AlertTemplateEntity mainTpl = new AlertTemplateEntity(
                UUID.randomUUID(), "main_menu", "Main Menu", "UTILITY", "en_US", "Main Menu", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
        );
        AlertButtonEntity button = new AlertButtonEntity(
                UUID.randomUUID(), mainTpl, "Help", ButtonType.QUICK_REPLY, "HELP", 1
        );
        button.setReply(new AlertButtonReplyEntity(UUID.randomUUID(), button, targetTpl, false));
        mainTpl.getButtons().add(button);

        AlertMessageEntity msg = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", mainTpl.getContent(), mainTpl, "wamid_tpl_2", AlertMessageStatus.SENT, null, null, Instant.now()
        );
        messageRepo.persist(msg);

        ChatTimelineResponseDTO timeline = timelineService.getTimeline(new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, 10));
        assertNotNull(timeline);
        assertEquals("menu_help_reply", timeline.messages().get(0).buttons().get(0).replyTemplateKey());
        assertFalse(timeline.messages().get(0).buttons().get(0).requiresHuman());
    }

    @Test
    @DisplayName("Should return all messages when limit is null or non-positive")
    void testGetTimeline_nullOrNegativeLimit() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            messageRepo.persist(new AlertMessageEntity(
                    UUID.randomUUID(), userId, AlertChannel.WHATSAPP, AlertDirection.INBOUND,
                    "TEXT", "Msg " + i, "wamid_" + i, AlertMessageStatus.DELIVERED, null, null, Instant.now().plusSeconds(i)
            ));
        }

        ChatTimelineResponseDTO tNull = timelineService.getTimeline(new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, null));
        assertEquals(3, tNull.messages().size());

        ChatTimelineResponseDTO tNeg = timelineService.getTimeline(new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, -1));
        assertEquals(3, tNeg.messages().size());
    }

    @Test
    @DisplayName("Should throw ValidationException when request or userId is null")
    void testGetTimeline_nullGuards() {
        assertThrows(ValidationException.class, () -> timelineService.getTimeline(null));
        assertThrows(ValidationException.class, () -> timelineService.getTimeline(new ChatTimelineRequestDTO(null, null, null)));
    }

    @Test
    @DisplayName("Should throw ValidationException when channel parameter is null")
    void testGetTimeline_nullChannel() {
        UUID userId = UUID.randomUUID();
        assertThrows(ValidationException.class, () -> timelineService.getTimeline(new ChatTimelineRequestDTO(userId, null, 50)));
    }

    @Test
    @DisplayName("sendStaffReply sends staff message and clears active human session")
    void testSendStaffReply_success() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210", false, true, Instant.now().plusSeconds(3600)
        );
        contactRepo.persist(contact);

        AdminChatReplyRequestDTO req = new AdminChatReplyRequestDTO(userId, AlertChannel.WHATSAPP, "Staff response text");
        ChatMessageDTO dto = timelineService.sendStaffReply(req);

        assertNotNull(dto);
        assertEquals(AlertChannel.WHATSAPP, dto.channel());
        assertEquals("Staff response text", dto.content());
        assertNull(contact.getHumanSessionExpiresAt());
    }

    @Test
    @DisplayName("sendStaffReply sends staff message when contact does not exist")
    void testSendStaffReply_noExistingContact() {
        UUID userId = UUID.randomUUID();
        AdminChatReplyRequestDTO req = new AdminChatReplyRequestDTO(userId, AlertChannel.WHATSAPP, "Reply with no prior contact record");
        ChatMessageDTO dto = timelineService.sendStaffReply(req);

        assertNotNull(dto);
        assertEquals("Reply with no prior contact record", dto.content());
    }

    @Test
    @DisplayName("sendStaffReply sends staff media reply and populates media fields")
    void testSendStaffReply_withMedia() {
        UUID userId = UUID.randomUUID();
        AdminChatReplyRequestDTO req = new AdminChatReplyRequestDTO(
                userId, AlertChannel.WHATSAPP, "Invoice copy", "/alerts/media/inv_99", "inv_99", "DOCUMENT"
        );
        ChatMessageDTO dto = timelineService.sendStaffReply(req);

        assertNotNull(dto);
        assertEquals("DOCUMENT", dto.messageType());
        assertEquals("/alerts/media/inv_99", dto.mediaUrl());
        assertEquals("inv_99", dto.mediaId());
        assertEquals("Invoice copy", dto.content());
    }

    @Test
    @DisplayName("Should filter out READ messages when unreadOnly is true")
    void testGetTimeline_unreadOnlyFilter() {
        UUID userId = UUID.randomUUID();
        // 1 read message
        AlertMessageEntity m1 = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.PUSH, AlertDirection.OUTBOUND,
                "TEMPLATE", "Old Read Message", null, null,
                AlertMessageStatus.READ, null, null, Instant.now().minusSeconds(200)
        );
        // 2 unread messages
        AlertMessageEntity m2 = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.PUSH, AlertDirection.OUTBOUND,
                "TEMPLATE", "Unread Message 1", null, null,
                AlertMessageStatus.DELIVERED, null, null, Instant.now().minusSeconds(100)
        );
        AlertMessageEntity m3 = new AlertMessageEntity(
                UUID.randomUUID(), userId, AlertChannel.PUSH, AlertDirection.OUTBOUND,
                "TEMPLATE", "Unread Message 2", null, null,
                AlertMessageStatus.DELIVERED, null, null, Instant.now()
        );
        messageRepo.persist(m1);
        messageRepo.persist(m2);
        messageRepo.persist(m3);

        // Query with unreadOnly = true
        ChatTimelineResponseDTO unreadTimeline = timelineService.getTimeline(
                new ChatTimelineRequestDTO(userId, AlertChannel.PUSH, 10, true)
        );
        assertEquals(2, unreadTimeline.messages().size());
        assertEquals("Unread Message 1", unreadTimeline.messages().get(0).content());
        assertEquals("Unread Message 2", unreadTimeline.messages().get(1).content());

        // Query with unreadOnly = false (all messages)
        ChatTimelineResponseDTO allTimeline = timelineService.getTimeline(
                new ChatTimelineRequestDTO(userId, AlertChannel.PUSH, 10, false)
        );
        assertEquals(3, allTimeline.messages().size());
    }

    @Test
    @DisplayName("Default 0-arg constructor instantiates successfully")
    void testDefaultConstructor() {
        ChatTimelineService service = new ChatTimelineService();
        assertNotNull(service);
    }

    static class FakeAlertMessageRepository extends AlertMessageRepository {
        final List<AlertMessageEntity> storage = new ArrayList<>();

        @Override
        public void persist(AlertMessageEntity entity) {
            storage.add(entity);
        }

        @Override
        public List<AlertMessageEntity> findByUserId(UUID userId) {
            return storage.stream().filter(m -> m.getUserId().equals(userId)).toList();
        }

        @Override
        public List<AlertMessageEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            return storage.stream().filter(m -> m.getUserId().equals(userId) && m.getChannel() == channel).toList();
        }

        @Override
        public List<AlertMessageEntity> findUnreadByUserAndChannel(UUID userId, AlertChannel channel) {
            return storage.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getChannel() == channel && m.getStatus() != AlertMessageStatus.READ)
                    .toList();
        }

        @Override
        public List<AlertMessageEntity> findUnreadByUserId(UUID userId) {
            return storage.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getStatus() != AlertMessageStatus.READ)
                    .toList();
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

        @Override
        public List<UserContactEntity> findByUserId(UUID userId) {
            return contacts.values().stream().filter(c -> c.getUserId().equals(userId)).toList();
        }

        @Override
        public void persist(UserContactEntity entity) {
            contacts.put(key(entity.getUserId(), entity.getChannel()), entity);
        }

        @Override
        public UserContactEntity merge(UserContactEntity entity) {
            contacts.put(key(entity.getUserId(), entity.getChannel()), entity);
            return entity;
        }
    }
}
