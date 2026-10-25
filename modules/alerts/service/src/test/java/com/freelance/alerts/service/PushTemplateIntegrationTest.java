package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.*;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.push.PushAlertProcessor;
import com.freelance.alerts.templates.SupportAccessRequestedTemplate;
import com.freelance.chat.ChatTimelineService;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.template.TemplateService;
import com.freelance.template.assembler.TemplateAssembler;
import com.freelance.template.seeder.TemplateSeeder;
import com.freelance.user.UserDTO;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;
import com.freelance.user.read.UserReadService;
import com.freelance.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Push Template & Timeline Integration Tests")
class PushTemplateIntegrationTest {

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

        @Override
        public List<AlertTemplateEntity> findAllTemplates() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            storage.put(entity.getId(), entity);
        }

        @Override
        public AlertTemplateEntity merge(AlertTemplateEntity entity) {
            storage.put(entity.getId(), entity);
            return entity;
        }
    }

    static class FakeButtonRepository extends AlertButtonRepository {}

    static class FakeCategoryRepository extends AlertTemplateCategoryRepository {
        final Map<String, AlertTemplateCategoryEntity> categories = new HashMap<>();

        @Override
        public Optional<AlertTemplateCategoryEntity> findByKey(String key) {
            return Optional.ofNullable(categories.get(key.toUpperCase()));
        }
    }

    static class FakeMessageRepository extends AlertMessageRepository {
        final List<AlertMessageEntity> messages = new ArrayList<>();

        @Override
        public void persist(AlertMessageEntity entity) {
            messages.add(entity);
        }

        @Override
        public AlertMessageEntity get(UUID id) {
            return messages.stream()
                    .filter(m -> m.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public AlertMessageEntity merge(AlertMessageEntity entity) {
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId().equals(entity.getId())) {
                    messages.set(i, entity);
                    return entity;
                }
            }
            messages.add(entity);
            return entity;
        }

        @Override
        public List<AlertMessageEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            return messages.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getChannel() == channel)
                    .toList();
        }

        @Override
        public List<AlertMessageEntity> findUnreadByUserAndChannel(UUID userId, AlertChannel channel) {
            return messages.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getChannel() == channel && m.getStatus() != AlertMessageStatus.READ)
                    .toList();
        }

        @Override
        public List<AlertMessageEntity> findByUserId(UUID userId) {
            return messages.stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public List<AlertMessageEntity> findUnreadByUserId(UUID userId) {
            return messages.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getStatus() != AlertMessageStatus.READ)
                    .toList();
        }

        @Override
        public List<AlertMessageEntity> findRecentByUserAndChannel(UUID userId, AlertChannel channel, Integer limit, Instant beforeTimestamp) {
            return messages.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getChannel() == channel)
                    .filter(m -> beforeTimestamp == null || (m.getTimestamp() != null && m.getTimestamp().isBefore(beforeTimestamp)))
                    .limit(limit != null && limit > 0 ? limit : Long.MAX_VALUE)
                    .toList();
        }
    }

    static class FakeContactRepository extends UserContactRepository {
        @Override
        public Optional<UserContactEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            return Optional.empty();
        }

        @Override
        public List<UserContactEntity> findByUserId(UUID userId) {
            return Collections.emptyList();
        }
    }

    private FakeTemplateRepository templateRepository;
    private FakeButtonRepository buttonRepository;
    private FakeCategoryRepository categoryRepository;
    private FakeMessageRepository messageRepository;
    private FakeContactRepository contactRepository;

    private TemplateService templateService;
    private ChatTimelineService chatTimelineService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        templateRepository = new FakeTemplateRepository();
        buttonRepository = new FakeButtonRepository();
        categoryRepository = new FakeCategoryRepository();
        messageRepository = new FakeMessageRepository();
        contactRepository = new FakeContactRepository();

        categoryRepository.categories.put("AUTHENTICATION", new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "AUTHENTICATION", "Authentication", "Security alerts and OTP", "HIGH"
        ));
        categoryRepository.categories.put("UTILITY", new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "UTILITY", "Utility", "Transactional updates", "NORMAL"
        ));
        categoryRepository.categories.put("MARKETING", new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "MARKETING", "Marketing", "Promotions and announcements", "LOW"
        ));

        templateService = new TemplateService(
                templateRepository,
                buttonRepository,
                categoryRepository,
                new TemplateAssembler()
        );

        chatTimelineService = new ChatTimelineService(
                messageRepository,
                contactRepository,
                new WhatsAppDispatcherService(messageRepository)
        );
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Create Push template is immediately APPROVED without Meta API submission")
    void testCreatePushTemplateApprovedImmediately() {
        TemplateCreateRequestDTO request = new TemplateCreateRequestDTO(
                "job_matching_alert",
                "Job Matching Alert",
                "UTILITY",
                "HIGH",
                "en_US",
                "A new freelance gig matching your skills is available!",
                null,
                null,
                AlertChannel.PUSH,
                null,
                null,
                null
        );

        TemplateDTO created = templateService.createTemplate(request);

        assertNotNull(created);
        assertEquals("job_matching_alert", created.templateKey());
        assertEquals(AlertChannel.PUSH, created.channel());
        assertEquals(TemplateStatus.APPROVED, created.status());
    }

    @Test
    @DisplayName("Update Push template retains APPROVED status")
    void testUpdatePushTemplateRetainsApproved() {
        UUID id = UUID.randomUUID();
        AlertTemplateEntity entity = new AlertTemplateEntity(
                id,
                "job_alert",
                "Job Alert",
                "UTILITY",
                "en_US",
                "Old content",
                null,
                null,
                AlertChannel.PUSH,
                TemplateStatus.APPROVED,
                false,
                null,
                null,
                "NORMAL"
        );
        templateRepository.storage.put(id, entity);

        TemplateUpdateRequestDTO updateRequest = new TemplateUpdateRequestDTO(
                id,
                "job_alert",
                "Updated Job Alert",
                "UTILITY",
                "HIGH",
                "en_US",
                "New content for jobs",
                null,
                null,
                null,
                null
        );

        TemplateDTO updated = templateService.updateTemplate(updateRequest);

        assertNotNull(updated);
        assertEquals("New content for jobs", updated.content());
        assertEquals(TemplateStatus.APPROVED, updated.status());
    }

    @Test
    @DisplayName("Seeder successfully registers built-in Push templates")
    void testSeedPushTemplates() {
        TemplateSeeder seeder = new TemplateSeeder(templateRepository);
        seeder.seed(List.of(
                new SupportAccessRequestedTemplate()
        ));

        assertEquals(1, templateRepository.storage.size());

        Optional<AlertTemplateEntity> supportReq = templateRepository.findByKey("support_access_requested");
        assertTrue(supportReq.isPresent());
        assertEquals(AlertChannel.PUSH, supportReq.get().getChannel());
        assertEquals(TemplateStatus.APPROVED, supportReq.get().getStatus());
    }

    @Test
    @DisplayName("Timeline API returns push notifications when queried with channel = PUSH")
    void testTimelineReturnsPushNotifications() {
        UUID userId = UUID.randomUUID();

        // 1 WhatsApp message
        messageRepository.persist(new AlertMessageEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                AlertDirection.OUTBOUND,
                "TEXT",
                "WhatsApp Message",
                null,
                AlertMessageStatus.DELIVERED,
                null,
                null,
                Instant.now().minusSeconds(100)
        ));

        // 2 Push notifications
        messageRepository.persist(new AlertMessageEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.PUSH,
                AlertDirection.OUTBOUND,
                "NOTIFICATION",
                "Push Notification 1",
                null,
                AlertMessageStatus.DELIVERED,
                null,
                null,
                Instant.now().minusSeconds(50)
        ));
        messageRepository.persist(new AlertMessageEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.PUSH,
                AlertDirection.OUTBOUND,
                "NOTIFICATION",
                "Push Notification 2",
                null,
                AlertMessageStatus.DELIVERED,
                null,
                null,
                Instant.now()
        ));

        // Query timeline for PUSH channel
        ChatTimelineResponseDTO pushTimeline = chatTimelineService.getTimeline(
                new ChatTimelineRequestDTO(userId, AlertChannel.PUSH, 10)
        );

        assertNotNull(pushTimeline);
        assertEquals(userId, pushTimeline.userId());
        assertFalse(pushTimeline.isHumanRequired());
        assertEquals(2, pushTimeline.messages().size());
        assertEquals("Push Notification 1", pushTimeline.messages().get(0).content());
        assertEquals("Push Notification 2", pushTimeline.messages().get(1).content());
        assertEquals(AlertChannel.PUSH, pushTimeline.messages().get(0).channel());

        // Query timeline for WHATSAPP channel
        ChatTimelineResponseDTO waTimeline = chatTimelineService.getTimeline(
                new ChatTimelineRequestDTO(userId, AlertChannel.WHATSAPP, 10)
        );

        assertNotNull(waTimeline);
        assertEquals(1, waTimeline.messages().size());
        assertEquals("WhatsApp Message", waTimeline.messages().get(0).content());
        assertEquals(AlertChannel.WHATSAPP, waTimeline.messages().get(0).channel());
    }

    @Test
    @DisplayName("Full Push Notification Lifecycle: delivery -> unread timeline -> inbound read -> excluded from unread timeline")
    void testPushNotificationLifecycle_delivery_inboundRead_timelineFiltering() {
        UUID userId = UUID.randomUUID();

        UserReadService fakeUserRead = new UserReadService() {
            @Override
            public UserReadResponseDTO readUser(UserReadRequestDTO request) {
                return new UserReadResponseDTO(new UserDTO(
                        request.id(), "test_user", "hash", java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
                ));
            }
        };

        PushAlertProcessor pushProcessor = new PushAlertProcessor(
                fakeUserRead,
                templateRepository,
                messageRepository,
                null
        );

        // 1. Seed Push templates
        TemplateSeeder seeder = new TemplateSeeder(templateRepository);
        seeder.seed(List.of(new SupportAccessRequestedTemplate()));
        AlertTemplateEntity template = templateRepository.findByKey("support_access_requested").orElseThrow();

        // 2. Deliver notification via PushAlertProcessor
        UUID msgId = UUID.randomUUID();
        AlertEventPayloadDTO outboundPayload = new AlertEventPayloadDTO(template.getId(), userId, null, msgId);
        pushProcessor.processOutbound(EventMessage.of("alert.outbound.push", outboundPayload, EventPriority.HIGH));

        // Verify message is saved with DELIVERED status
        AlertMessageEntity persisted = messageRepository.get(msgId);
        assertNotNull(persisted);
        assertEquals(AlertMessageStatus.DELIVERED, persisted.getStatus());

        // 3. Query timeline with unreadOnly = true
        ChatTimelineResponseDTO unreadTimelineBefore = chatTimelineService.getTimeline(
                new ChatTimelineRequestDTO(userId, AlertChannel.PUSH, 10, true)
        );
        assertEquals(1, unreadTimelineBefore.messages().size());
        assertEquals(msgId, unreadTimelineBefore.messages().get(0).id());
        assertEquals(AlertMessageStatus.DELIVERED, unreadTimelineBefore.messages().get(0).status());

        // 4. Inbound read event: mark notification as READ
        NotificationReadRequestDTO readPayload = new NotificationReadRequestDTO(userId, List.of(msgId), AlertChannel.PUSH);
        pushProcessor.processInbound(EventMessage.of("alert.inbound.push", readPayload, EventPriority.HIGH));

        // Verify message status is updated to READ
        assertEquals(AlertMessageStatus.READ, messageRepository.get(msgId).getStatus());

        // 5. Query timeline with unreadOnly = true -> should be empty
        ChatTimelineResponseDTO unreadTimelineAfter = chatTimelineService.getTimeline(
                new ChatTimelineRequestDTO(userId, AlertChannel.PUSH, 10, true)
        );
        assertTrue(unreadTimelineAfter.messages().isEmpty(), "Read notification must not appear when unreadOnly is true");

        // 6. Query timeline with unreadOnly = false -> should return the notification with status READ
        ChatTimelineResponseDTO allTimeline = chatTimelineService.getTimeline(
                new ChatTimelineRequestDTO(userId, AlertChannel.PUSH, 10, false)
        );
        assertEquals(1, allTimeline.messages().size());
        assertEquals(AlertMessageStatus.READ, allTimeline.messages().get(0).status());
    }
}
