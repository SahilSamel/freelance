package com.freelance.alerts.constraint;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateUpdateRequestDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.templates.InboundWelcomeTemplate;
import com.freelance.fixtures.FakeAlertTemplateCategoryRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.ConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.template.constraint.TemplateUpdateConstraint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TemplateUpdateConstraintTest {

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<UUID, AlertTemplateEntity> byId = new HashMap<>();
        final Map<String, AlertTemplateEntity> byKey = new HashMap<>();

        @Override
        public AlertTemplateEntity get(UUID id) {
            return byId.get(id);
        }

        @Override
        public Optional<AlertTemplateEntity> findByKey(String templateKey) {
            if (templateKey == null || templateKey.isBlank()) return Optional.empty();
            return Optional.ofNullable(byKey.get(templateKey));
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            byId.put(entity.getId(), entity);
            byKey.put(entity.getTemplateKey(), entity);
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
    private FakeAlertTemplateCategoryRepository categoryRepo;
    private TemplateUpdateConstraint constraint;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        templateRepo = new FakeAlertTemplateRepository();
        buttonRepo = new FakeAlertButtonRepository();
        categoryRepo = new FakeAlertTemplateCategoryRepository();
        constraint = new TemplateUpdateConstraint(templateRepo, buttonRepo, categoryRepo);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    private AlertTemplateEntity createSampleTemplate(UUID id, String key) {
        AlertTemplateEntity tpl = new AlertTemplateEntity(
                id, key, "Sample", "UTILITY", "en", "Content", null, null,
                AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null
        );
        templateRepo.persist(tpl);
        return tpl;
    }

    @Test
    @DisplayName("Should throw ConstraintException when template does not exist")
    void testTemplateNotFoundThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(UUID.randomUUID(), "Updated");
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(ConstraintErrors.TEMPLATE_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Should throw ConstraintException when category does not exist")
    void testCategoryNotFoundThrows() {
        UUID id = UUID.randomUUID();
        createSampleTemplate(id, "my_template");

        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                id, null, "Sample", "NON_EXISTENT_CAT", "en", "Content", null, null, null, null
        );
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(ConstraintErrors.TEMPLATE_CATEGORY_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Should reject welcome menu update if no button has requiresHuman=true")
    void testWelcomeMenuRequiresHumanButton() {
        UUID id = UUID.randomUUID();
        createSampleTemplate(id, InboundWelcomeTemplate.TEMPLATE_KEY);

        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                id, null, "Inbound Welcome Menu", null, null, "Hello! Choose an option:", null, null, null,
                List.of(
                        new TemplateButtonDTO("Option 1", ButtonType.URL, "https://example.com", null, false, 1),
                        new TemplateButtonDTO("Option 2", ButtonType.PHONE_NUMBER, "+919876543210", null, false, 2)
                )
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(ConstraintErrors.HUMAN_ESCALATION_BUTTON_REQUIRED, ex.getError());
    }

    @Test
    @DisplayName("Should allow welcome menu update when at least one button has requiresHuman=true")
    void testWelcomeMenuPassesWithHumanButton() {
        UUID id = UUID.randomUUID();
        createSampleTemplate(id, InboundWelcomeTemplate.TEMPLATE_KEY);

        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                id, null, "Inbound Welcome Menu", null, null, "Hello! Choose an option:", null, null, null,
                List.of(
                        new TemplateButtonDTO("Talk to Staff", ButtonType.QUICK_REPLY, "TALK_STAFF", null, true, 1),
                        new TemplateButtonDTO("Visit Website", ButtonType.URL, "https://example.com", null, false, 2)
                )
        );

        AlertTemplateEntity target = assertDoesNotThrow(() -> constraint.checkConstraints(req));
        assertNotNull(target);
    }

    @Test
    @DisplayName("Should throw ConstraintException when button payload collides with another template")
    void testButtonPayloadCollisionWithOtherTemplateThrows() {
        UUID id1 = UUID.randomUUID();
        AlertTemplateEntity tpl1 = createSampleTemplate(id1, "template_1");

        UUID id2 = UUID.randomUUID();
        createSampleTemplate(id2, "template_2");

        // Button belonging to tpl1
        AlertButtonEntity existingBtn = new AlertButtonEntity(UUID.randomUUID(), tpl1, "Old", ButtonType.QUICK_REPLY, "SHARED_PAYLOAD", 1);
        buttonRepo.persist(existingBtn);

        // Try updating tpl2 to use SHARED_PAYLOAD
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                id2, null, "Template 2", null, null, "Content", null, null, null,
                List.of(new TemplateButtonDTO("Option", ButtonType.QUICK_REPLY, "SHARED_PAYLOAD", null, true, 1))
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(ConstraintErrors.BUTTON_PAYLOAD_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("Should NOT throw payload collision when updating template's own existing button payload")
    void testButtonPayloadSameTemplatePasses() {
        UUID id = UUID.randomUUID();
        AlertTemplateEntity tpl = createSampleTemplate(id, "template_1");

        // Button belonging to tpl
        AlertButtonEntity existingBtn = new AlertButtonEntity(UUID.randomUUID(), tpl, "Old", ButtonType.QUICK_REPLY, "MY_PAYLOAD", 1);
        buttonRepo.persist(existingBtn);

        // Update tpl keeping MY_PAYLOAD
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                id, null, "Template 1", null, null, "New Content", null, null, null,
                List.of(new TemplateButtonDTO("Updated Text", ButtonType.QUICK_REPLY, "MY_PAYLOAD", null, true, 1))
        );

        AlertTemplateEntity result = assertDoesNotThrow(() -> constraint.checkConstraints(req));
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should throw ConstraintException when non-terminal button references non-existent reply template")
    void testNonTerminalButtonBrokenChainThrows() {
        UUID id = UUID.randomUUID();
        createSampleTemplate(id, "template_1");

        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                id, null, "Template 1", null, null, "Content", null, null, null,
                List.of(new TemplateButtonDTO("Go", ButtonType.QUICK_REPLY, "GO_PAYLOAD", "NON_EXISTENT_REPLY", false, 1))
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(ConstraintErrors.TEMPLATE_CHAIN_BROKEN, ex.getError());
    }
}
