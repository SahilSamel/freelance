package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateCreateRequestDTO;
import com.freelance.alerts.dto.TemplateDTO;
import com.freelance.alerts.dto.TemplateListRequestDTO;
import com.freelance.alerts.dto.TemplateListResponseDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.template.assembler.TemplateAssembler;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.template.TemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.freelance.dispatcher.impl.whatsapp.WhatsAppTemplateService;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppTemplateService.TemplateSubmissionResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TemplateServiceTest {

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<String, AlertTemplateEntity> byKey = new HashMap<>();
        final Map<UUID, AlertTemplateEntity> byId = new HashMap<>();

        @Override
        public Optional<AlertTemplateEntity> findByKey(String templateKey) {
            if (templateKey == null || templateKey.isBlank()) return Optional.empty();
            return Optional.ofNullable(byKey.get(templateKey));
        }

        @Override
        public AlertTemplateEntity get(UUID id) {
            return byId.get(id);
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            byKey.put(entity.getTemplateKey(), entity);
            byId.put(entity.getId(), entity);
        }

        @Override
        public AlertTemplateEntity merge(AlertTemplateEntity entity) {
            byKey.put(entity.getTemplateKey(), entity);
            byId.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public List<AlertTemplateEntity> findAllTemplates() {
            return new ArrayList<>(byKey.values());
        }

        @Override
        public List<AlertTemplateEntity> findByStatus(TemplateStatus status) {
            return byKey.values().stream().filter(e -> e.getStatus() == status).toList();
        }
    }

    static class FakeAlertButtonRepository extends AlertButtonRepository {
        final Map<String, AlertButtonEntity> byPayload = new HashMap<>();

        @Override
        public Optional<AlertButtonEntity> findByPayload(String payload) {
            if (payload == null || payload.isBlank()) return Optional.empty();
            return Optional.ofNullable(byPayload.get(payload));
        }

        @Override
        public void persist(AlertButtonEntity entity) {
            if (entity.getPayload() != null) {
                byPayload.put(entity.getPayload(), entity);
            }
        }
    }

    private TransactionContext context;
    private FakeAlertTemplateRepository templateRepo;
    private FakeAlertButtonRepository buttonRepo;
    private com.freelance.fixtures.FakeAlertTemplateCategoryRepository categoryRepo;
    private TemplateAssembler assembler;
    private TemplateService service;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        templateRepo = new FakeAlertTemplateRepository();
        buttonRepo = new FakeAlertButtonRepository();
        categoryRepo = new com.freelance.fixtures.FakeAlertTemplateCategoryRepository();
        assembler = new TemplateAssembler();
        service = new TemplateService(templateRepo, buttonRepo, categoryRepo, assembler);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should create single terminal template successfully")
    void testCreateSingleTemplate() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_WELCOME", "Welcome", "UTILITY", "en", "Hello {{USER.name}}",
                null, null, AlertChannel.WHATSAPP, Set.of("USER.name"), null
        );

        TemplateDTO created = service.createTemplate(req);

        assertNotNull(created);
        assertEquals("TPL_WELCOME", created.templateKey());
        assertEquals("Welcome", created.name());
        assertEquals("Hello {{USER.name}}", created.content());
        assertTrue(created.requiredAttributes().contains("USER.name"));
        assertTrue(templateRepo.byKey.containsKey("TPL_WELCOME"));
    }

    @Test
    @DisplayName("Should create template with button linking to another template in the same request")
    void testCreateTemplateWithRequestReplyTemplate() {
        TemplateCreateRequestDTO child = new TemplateCreateRequestDTO(
                "TPL_STEP_2", "Step 2", "UTILITY", "en", "Confirmation step",
                null, null, AlertChannel.WHATSAPP, null, null
        );
        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_STEP_1", "Step 1", "UTILITY", "en", "Start step",
                null, null, AlertChannel.WHATSAPP, null,
                List.of(new TemplateButtonDTO("Next", ButtonType.QUICK_REPLY, "p_next", "TPL_STEP_2", false, 1)),
                List.of(child)
        );

        TemplateDTO created = service.createTemplate(root);

        assertNotNull(created);
        assertEquals("TPL_STEP_1", created.templateKey());
        assertEquals(1, created.buttons().size());
        assertEquals("TPL_STEP_2", created.buttons().get(0).replyTemplateKey());

        AlertTemplateEntity rootEntity = templateRepo.byKey.get("TPL_STEP_1");
        assertNotNull(rootEntity);
        assertEquals(1, rootEntity.getButtons().size());
        AlertButtonEntity btn = rootEntity.getButtons().get(0);
        assertNotNull(btn.getReply());
        assertNotNull(btn.getReply().getReplyTemplate());
        assertEquals("TPL_STEP_2", btn.getReply().getReplyTemplate().getTemplateKey());
    }

    @Test
    @DisplayName("Should create template with button linking to existing template in database")
    void testCreateTemplateWithDatabaseReplyTemplate() {
        AlertTemplateEntity existingEntity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_EXISTING_MENU", "Menu", "UTILITY", "en",
                "Existing menu content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED,
                false, null, null
        );
        templateRepo.persist(existingEntity);

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_NEW_TRIGGER", "Trigger", "UTILITY", "en", "Go to menu below",
                null, null, AlertChannel.WHATSAPP, null,
                List.of(new TemplateButtonDTO("Menu", ButtonType.QUICK_REPLY, "p_menu", "TPL_EXISTING_MENU", false, 1))
        );

        TemplateDTO created = service.createTemplate(req);

        assertNotNull(created);
        assertEquals("TPL_NEW_TRIGGER", created.templateKey());
        assertEquals(1, created.buttons().size());
        assertEquals("TPL_EXISTING_MENU", created.buttons().get(0).replyTemplateKey());

        AlertTemplateEntity savedEntity = templateRepo.byKey.get("TPL_NEW_TRIGGER");
        assertNotNull(savedEntity);
        assertEquals(1, savedEntity.getButtons().size());
        AlertButtonEntity btn = savedEntity.getButtons().get(0);
        assertNotNull(btn.getReply());
        assertNotNull(btn.getReply().getReplyTemplate());
        assertEquals("TPL_EXISTING_MENU", btn.getReply().getReplyTemplate().getTemplateKey());
        assertEquals(existingEntity.getId(), btn.getReply().getReplyTemplate().getId());
    }

    @Test
    @DisplayName("Should throw ConstraintException when button references key not in request and not in DB")
    void testCreateTemplateWithBrokenReplyLinkThrows() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_BROKEN", "Broken", "UTILITY", "en", "Broken chain",
                null, null, AlertChannel.WHATSAPP, null,
                List.of(new TemplateButtonDTO("Broken", ButtonType.QUICK_REPLY, "p", "UNKNOWN_KEY", false, 1))
        );

        assertThrows(ConstraintException.class, () -> service.createTemplate(req));
    }

    @Test
    @DisplayName("Should throw ConstraintException when template key already exists in database")
    void testCreateTemplateDuplicateKeyThrows() {
        AlertTemplateEntity existingEntity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_ALREADY_EXISTS", "Existing", "UTILITY", "en",
                "Existing", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED,
                false, null, null
        );
        templateRepo.persist(existingEntity);

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_ALREADY_EXISTS", "New With Same Key", "UTILITY", "en", "Duplicate key text",
                null, null, AlertChannel.WHATSAPP, null, null
        );

        assertThrows(ConstraintException.class, () -> service.createTemplate(req));
    }

    @Test
    @DisplayName("Should list and get templates correctly")
    void testListAndGetTemplates() {
        UUID id1 = UUID.randomUUID();
        AlertTemplateEntity entity1 = new AlertTemplateEntity(
                id1, "TPL_1", "One", "UTILITY", "en", "Content 1",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null
        );
        UUID id2 = UUID.randomUUID();
        AlertTemplateEntity entity2 = new AlertTemplateEntity(
                id2, "TPL_2", "Two", "UTILITY", "en", "Content 2",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.DRAFT, false, null, null
        );
        templateRepo.persist(entity1);
        templateRepo.persist(entity2);

        TemplateListResponseDTO allResp = service.listTemplates(null);
        assertEquals(2, allResp.templates().size());

        TemplateListResponseDTO approvedResp = service.listTemplates(new TemplateListRequestDTO(null, TemplateStatus.APPROVED));
        assertEquals(1, approvedResp.templates().size());
        assertEquals("TPL_1", approvedResp.templates().get(0).templateKey());

        TemplateDTO single = service.getTemplate(id1);
        assertNotNull(single);
        assertEquals("TPL_1", single.templateKey());
    }

    @Test
    @DisplayName("Should throw ConstraintException when button payload already exists in database")
    void testCreateTemplateDuplicateButtonPayloadInDBThrows() {
        AlertButtonEntity existingBtn = new AlertButtonEntity(
                UUID.randomUUID(), null, "Existing", ButtonType.QUICK_REPLY, "PAYLOAD_TAKEN", 1
        );
        buttonRepo.persist(existingBtn);

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_NEW_WITH_DUP_BTN", "Title", "UTILITY", "en", "Message",
                null, null, AlertChannel.WHATSAPP, null,
                List.of(new TemplateButtonDTO("Click", ButtonType.QUICK_REPLY, "PAYLOAD_TAKEN", null, true, 1))
        );

        assertThrows(ConstraintException.class, () -> service.createTemplate(req));
    }

    @Test
    @DisplayName("Should list all active categories from repository")
    void testListCategories() {
        var response = service.listCategories();

        assertNotNull(response);
        assertNotNull(response.categories());
        assertEquals(3, response.categories().size());

        var authCat = response.categories().stream()
                .filter(c -> "AUTHENTICATION".equals(c.categoryKey()))
                .findFirst();
        assertTrue(authCat.isPresent());
        assertEquals("Authentication", authCat.get().name());
        assertEquals("HIGH", authCat.get().priority());
    }

    @Test
    @DisplayName("Should assign metaTemplateId to WhatsApp template in mock mode")
    void testCreateWhatsAppTemplatePopulatesMetaTemplateId() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_WA_META_ID", "WA Template", "UTILITY", "en_US", "Hello {{name}}",
                null, null, AlertChannel.WHATSAPP, null, null
        );

        TemplateDTO created = service.createTemplate(req);
        assertNotNull(created);
        assertEquals(TemplateStatus.SUBMITTED, created.status());

        AlertTemplateEntity entity = templateRepo.byKey.get("TPL_WA_META_ID");
        assertNotNull(entity);
        assertNotNull(entity.getMetaTemplateId());
        assertTrue(entity.getMetaTemplateId().startsWith("meta_tpl_"));
    }

    @Test
    @DisplayName("Should set SUBMITTED status when WhatsAppTemplateService returns SUBMITTED")
    void testCreateWhatsAppTemplateWithSubmittedStatus() {
        WhatsAppTemplateService customService = new WhatsAppTemplateService() {
            @Override
            public TemplateSubmissionResult submitTemplate(AlertTemplateEntity entity, List<TemplateButtonDTO> buttons) {
                return TemplateSubmissionResult.success("meta_live_12345", TemplateStatus.SUBMITTED);
            }
        };

        TemplateService testService = new TemplateService(templateRepo, buttonRepo, categoryRepo, assembler, customService);

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_WA_SUBMITTED", "Submitted Template", "UTILITY", "en_US", "Text",
                null, null, AlertChannel.WHATSAPP, null, null
        );

        TemplateDTO created = testService.createTemplate(req);
        assertNotNull(created);
        assertEquals(TemplateStatus.SUBMITTED, created.status());

        AlertTemplateEntity entity = templateRepo.byKey.get("TPL_WA_SUBMITTED");
        assertNotNull(entity);
        assertEquals("meta_live_12345", entity.getMetaTemplateId());
        assertEquals(TemplateStatus.SUBMITTED, entity.getStatus());
    }

    @Test
    @DisplayName("Should throw ConstraintException and not persist template when WhatsApp submission fails")
    void testCreateWhatsAppTemplateFailureThrows() {
        WhatsAppTemplateService failingService = new WhatsAppTemplateService() {
            @Override
            public TemplateSubmissionResult submitTemplate(AlertTemplateEntity entity, List<TemplateButtonDTO> buttons) {
                return TemplateSubmissionResult.failure("Meta Graph API rejected: name invalid");
            }
        };

        TemplateService testService = new TemplateService(templateRepo, buttonRepo, categoryRepo, assembler, failingService);

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_WA_FAIL", "Failing Template", "UTILITY", "en_US", "Text",
                null, null, AlertChannel.WHATSAPP, null, null
        );

        assertThrows(ConstraintException.class, () -> testService.createTemplate(req));
        assertFalse(templateRepo.byKey.containsKey("TPL_WA_FAIL"));
    }

    @Test
    @DisplayName("Should create template with priority and update priority successfully")
    void testCreateAndUpdateTemplatePriority() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_PRIORITY_TEST", "Priority Template", "UTILITY", "HIGH", "en", "Urgent alert",
                null, null, AlertChannel.WHATSAPP, null, null, Collections.emptyList()
        );

        TemplateDTO created = service.createTemplate(req);
        assertNotNull(created);
        assertEquals("HIGH", created.priority());
        assertEquals("UTILITY", created.category());

        AlertTemplateEntity savedEntity = templateRepo.byKey.get("TPL_PRIORITY_TEST");
        assertNotNull(savedEntity);
        assertEquals("HIGH", savedEntity.getPriority());

        // Update priority to NORMAL
        com.freelance.alerts.dto.TemplateUpdateRequestDTO updateReq = new com.freelance.alerts.dto.TemplateUpdateRequestDTO(
                savedEntity.getId(), null, null, null, "NORMAL", null, null, null, null, null, null
        );

        TemplateDTO updated = service.updateTemplate(updateReq);
        assertNotNull(updated);
        assertEquals("NORMAL", updated.priority());
        assertEquals("NORMAL", savedEntity.getPriority());
    }
}
