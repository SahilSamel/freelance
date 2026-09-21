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
import com.freelance.alerts.templates.HumanSupportRequiredTemplate;
import com.freelance.alerts.templates.ProfileReminderTemplate;
import com.freelance.alerts.templates.SupportAccessRequestedTemplate;
import com.freelance.chat.ChatTimelineService;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import com.freelance.template.TemplateService;
import com.freelance.template.assembler.TemplateAssembler;
import com.freelance.template.seeder.TemplateSeeder;
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
        public List<AlertMessageEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            return messages.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getChannel() == channel)
                    .toList();
        }

        @Override
        public List<AlertMessageEntity> findByUserId(UUID userId) {
            return messages.stream()
                    .filter(m -> m.getUserId().equals(userId))
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

        categoryRepository.categories.put("UTILITY", new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "UTILITY", "Utility", "Transactional alerts", "NORMAL"
        ));
        categoryRepository.categories.put("SUPPORT", new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "SUPPORT", "Support", "Support alerts", "HIGH"
        ));
        categoryRepository.categories.put("REMINDER", new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "REMINDER", "Reminder", "Reminders", "NORMAL"
        ));
        categoryRepository.categories.put("SECURITY", new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "SECURITY", "Security", "Security alerts", "HIGH"
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
                new HumanSupportRequiredTemplate(),
                new SupportAccessRequestedTemplate(),
                new ProfileReminderTemplate()
        ));

        assertEquals(3, templateRepository.storage.size());

        Optional<AlertTemplateEntity> humanReq = templateRepository.findByKey("human_support_required");
        assertTrue(humanReq.isPresent());
        assertEquals(AlertChannel.PUSH, humanReq.get().getChannel());
        assertEquals(TemplateStatus.APPROVED, humanReq.get().getStatus());
        assertEquals("HIGH", humanReq.get().getPriority());

        Optional<AlertTemplateEntity> supportReq = templateRepository.findByKey("support_access_requested");
        assertTrue(supportReq.isPresent());
        assertEquals(AlertChannel.PUSH, supportReq.get().getChannel());
        assertEquals(TemplateStatus.APPROVED, supportReq.get().getStatus());

        Optional<AlertTemplateEntity> reminder = templateRepository.findByKey("profile_incomplete_reminder");
        assertTrue(reminder.isPresent());
        assertEquals(AlertChannel.PUSH, reminder.get().getChannel());
        assertEquals(TemplateStatus.APPROVED, reminder.get().getStatus());
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
}
