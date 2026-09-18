package com.freelance.alerts.constraint;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateCreateRequestDTO;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.template.constraint.TemplateCreateConstraint;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.template.TemplateConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCreateConstraintTest {

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<String, AlertTemplateEntity> byKey = new HashMap<>();

        @Override
        public Optional<AlertTemplateEntity> findByKey(String templateKey) {
            if (templateKey == null || templateKey.isBlank()) return Optional.empty();
            return Optional.ofNullable(byKey.get(templateKey));
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
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
    private com.freelance.fixtures.FakeAlertTemplateCategoryRepository categoryRepo;
    private TemplateCreateConstraint constraint;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        templateRepo = new FakeAlertTemplateRepository();
        buttonRepo = new FakeAlertButtonRepository();
        categoryRepo = new com.freelance.fixtures.FakeAlertTemplateCategoryRepository();
        constraint = new TemplateCreateConstraint(templateRepo, buttonRepo, categoryRepo);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should pass constraints when template key and button payloads are unique")
    void testUniqueTemplateAndButtonsPass() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_UNIQUE", "Unique", "UTILITY", "en", "Content", null, null, null, null,
                List.of(new TemplateButtonDTO("B1", ButtonType.QUICK_REPLY, "PAYLOAD_1", null, true, 1))
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    @DisplayName("Should throw ConstraintException when template key already exists in database")
    void testExistingTemplateKeyThrows() {
        templateRepo.persist(new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_EXISTS", "Existing", "UTILITY", "en", "Content",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null
        ));

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_EXISTS", "New", "UTILITY", "en", "Content", null, null, null, null, null
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(TemplateConstraintErrors.TEMPLATE_KEY_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("Should throw ConstraintException when button payload already exists in database")
    void testExistingButtonPayloadThrows() {
        buttonRepo.persist(new AlertButtonEntity(
                UUID.randomUUID(), null, "Existing", ButtonType.QUICK_REPLY, "EXISTING_PAYLOAD", 1
        ));

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_NEW", "New", "UTILITY", "en", "Content", null, null, null, null,
                List.of(new TemplateButtonDTO("B1", ButtonType.QUICK_REPLY, "EXISTING_PAYLOAD", null, true, 1))
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(TemplateConstraintErrors.BUTTON_PAYLOAD_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("Should allow existing URL button payload across different templates")
    void testExistingUrlButtonPayloadAllowedAcrossTemplates() {
        buttonRepo.persist(new AlertButtonEntity(
                UUID.randomUUID(), null, "Existing URL", ButtonType.URL, "https://example.com", 1
        ));

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_NEW_URL", "New URL", "UTILITY", "en", "Content", null, null, null, null,
                List.of(new TemplateButtonDTO("Website", ButtonType.URL, "https://example.com", null, false, 1))
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    @DisplayName("Should allow existing PHONE_NUMBER button payload across different templates")
    void testExistingPhoneNumberButtonPayloadAllowedAcrossTemplates() {
        buttonRepo.persist(new AlertButtonEntity(
                UUID.randomUUID(), null, "Existing Phone", ButtonType.PHONE_NUMBER, "+919876543210", 1
        ));

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_NEW_PHONE", "New Phone", "UTILITY", "en", "Content", null, null, null, null,
                List.of(new TemplateButtonDTO("Call", ButtonType.PHONE_NUMBER, "+919876543210", null, false, 1))
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    @DisplayName("Should ignore null or blank button payloads")
    void testNullOrBlankButtonPayloadsIgnored() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_NEW", "New", "UTILITY", "en", "Content", null, null, null, null,
                List.of(
                        new TemplateButtonDTO("B1", ButtonType.QUICK_REPLY, null, null, true, 1),
                        new TemplateButtonDTO("B2", ButtonType.QUICK_REPLY, "   ", null, true, 2)
                )
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    @DisplayName("Should check constraints for all templates in multi-template request")
    void testMultiTemplateConstraintsChecked() {
        buttonRepo.persist(new AlertButtonEntity(
                UUID.randomUUID(), null, "Existing", ButtonType.QUICK_REPLY, "TAKEN_PAYLOAD", 1
        ));

        TemplateCreateRequestDTO child = new TemplateCreateRequestDTO(
                "TPL_CHILD", "Child", "UTILITY", "en", "Child content", null, null, null, null,
                List.of(new TemplateButtonDTO("ChildBtn", ButtonType.QUICK_REPLY, "TAKEN_PAYLOAD", null, true, 1))
        );

        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_ROOT", "Root", "UTILITY", "en", "Root content", null, null, null, null,
                List.of(new TemplateButtonDTO("Next", ButtonType.QUICK_REPLY, "UNIQUE_ROOT_PAYLOAD", "TPL_CHILD", false, 1)),
                List.of(child)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(root));
        assertEquals(TemplateConstraintErrors.BUTTON_PAYLOAD_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("Should handle null request gracefully")
    void testNullRequestHandled() {
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
    }

    @Test
    @DisplayName("Should instantiate with 1-arg constructor gracefully")
    void testOneArgConstructor() {
        TemplateCreateConstraint oneArg = new TemplateCreateConstraint(templateRepo);
        assertNotNull(oneArg);
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_OK", "Ok", null, "en", "Content", null, null, null, null, null
        );
        assertDoesNotThrow(() -> oneArg.checkConstraints(req));
    }

    @Test
    @DisplayName("Should throw ConstraintException when category does not exist in database table")
    void testUnregisteredCategoryThrows() {
        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_INVALID_CAT", "Invalid", "NOT_A_REAL_CATEGORY", "en", "Content", null, null, null, null, null
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(TemplateConstraintErrors.TEMPLATE_CATEGORY_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Should pass constraints when category exists in database table")
    void testRegisteredCategoryPasses() {
        TemplateCreateRequestDTO reqAuth = new TemplateCreateRequestDTO(
                "TPL_CAT_AUTH", "Auth", "AUTHENTICATION", "en", "OTP code", null, null, null, null, null
        );
        assertDoesNotThrow(() -> constraint.checkConstraints(reqAuth));
    }

    @Test
    @DisplayName("Should pass constraints when custom category is dynamically added to database table")
    void testDynamicCustomCategoryInTablePasses() {
        categoryRepo.save(new com.freelance.alerts.template.entity.AlertTemplateCategoryEntity(
                UUID.randomUUID(), "SUPPORT", "Support & Care", "Support inquiries", "NORMAL"
        ));

        TemplateCreateRequestDTO req = new TemplateCreateRequestDTO(
                "TPL_CUSTOM_CAT", "Support", "SUPPORT", "en", "Ticket resolved", null, null, null, null, null
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    @DisplayName("Should pass constraints when button references existing terminal template in database")
    void testButtonReferencesExistingTerminalTemplatePasses() {
        AlertTemplateEntity existingEntity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_EXISTING_TERMINAL", "Existing Terminal", "UTILITY", "en",
                "Existing content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED,
                false, null, null
        );
        templateRepo.persist(existingEntity);

        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_NEW_ROOT", "Root", "UTILITY", "en", "Root message", null, null, null, null,
                List.of(new TemplateButtonDTO("Go to Existing", ButtonType.QUICK_REPLY, "p1", "TPL_EXISTING_TERMINAL", false, 1))
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(root));
    }

    @Test
    @DisplayName("Should pass constraints when button references existing template that chains to another existing template")
    void testButtonReferencesExistingTemplateWithChainPasses() {
        AlertTemplateEntity existingChild = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_EXISTING_CHILD", "Existing Child", "UTILITY", "en",
                "Child content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED,
                false, null, null
        );
        templateRepo.persist(existingChild);

        AlertTemplateEntity existingParent = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_EXISTING_PARENT", "Existing Parent", "UTILITY", "en",
                "Parent content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED,
                false, null, null
        );
        AlertButtonEntity btn = new AlertButtonEntity(
                UUID.randomUUID(), existingParent, "To Child", ButtonType.QUICK_REPLY, "p", 1
        );
        com.freelance.alerts.template.entity.AlertButtonReplyEntity reply = new com.freelance.alerts.template.entity.AlertButtonReplyEntity(
                UUID.randomUUID(), btn, existingChild, false
        );
        btn.setReply(reply);
        existingParent.getButtons().add(btn);
        templateRepo.persist(existingParent);

        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_NEW_ROOT", "Root", "UTILITY", "en", "Root message", null, null, null, null,
                List.of(new TemplateButtonDTO("Go to Parent", ButtonType.QUICK_REPLY, "p1", "TPL_EXISTING_PARENT", false, 1))
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(root));
    }

    @Test
    @DisplayName("Should pass constraints when button references existing template that loops back to request template")
    void testButtonReferencesExistingTemplateLoopsBackToRequestPasses() {
        AlertTemplateEntity existingEntity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_EXISTING_LOOP", "Existing Loop", "UTILITY", "en",
                "Loop content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED,
                false, null, null
        );
        AlertTemplateEntity stubRoot = new AlertTemplateEntity();
        stubRoot.setTemplateKey("TPL_ROOT");

        AlertButtonEntity btn = new AlertButtonEntity(
                UUID.randomUUID(), existingEntity, "Back to Root", ButtonType.QUICK_REPLY, "p_root", 1
        );
        com.freelance.alerts.template.entity.AlertButtonReplyEntity reply = new com.freelance.alerts.template.entity.AlertButtonReplyEntity(
                UUID.randomUUID(), btn, stubRoot, false
        );
        btn.setReply(reply);
        existingEntity.getButtons().add(btn);
        templateRepo.persist(existingEntity);

        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_ROOT", "Root", "UTILITY", "en", "Root message", null, null, null, null,
                List.of(new TemplateButtonDTO("To Existing", ButtonType.QUICK_REPLY, "p1", "TPL_EXISTING_LOOP", false, 1))
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(root));
    }

    @Test
    @DisplayName("Should pass constraints when existing template has terminal action buttons")
    void testButtonReferencesExistingTemplateTerminalActionsPasses() {
        AlertTemplateEntity existingEntity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_EXISTING_ACTIONS", "Actions", "UTILITY", "en",
                "Action content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED,
                false, null, null
        );

        AlertButtonEntity humanBtn = new AlertButtonEntity(
                UUID.randomUUID(), existingEntity, "Help", ButtonType.QUICK_REPLY, "p_help", 1
        );
        humanBtn.setReply(new com.freelance.alerts.template.entity.AlertButtonReplyEntity(UUID.randomUUID(), humanBtn, null, true));

        AlertButtonEntity urlBtn = new AlertButtonEntity(
                UUID.randomUUID(), existingEntity, "Site", ButtonType.URL, "https://example.com", 2
        );

        existingEntity.getButtons().addAll(List.of(humanBtn, urlBtn));
        templateRepo.persist(existingEntity);

        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_ROOT", "Root", "UTILITY", "en", "Root message", null, null, null, null,
                List.of(new TemplateButtonDTO("To Actions", ButtonType.QUICK_REPLY, "p1", "TPL_EXISTING_ACTIONS", false, 1))
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(root));
    }

    @Test
    @DisplayName("Should throw ConstraintException when button references a non-existent reply template key")
    void testButtonReferencesNonExistentReplyTemplateThrows() {
        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_ROOT", "Root", "UTILITY", "en", "Options", null, null, null, null,
                List.of(new TemplateButtonDTO("Broken", ButtonType.QUICK_REPLY, "p", "NON_EXISTENT_KEY", false, 1))
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(root));
        assertEquals(TemplateConstraintErrors.TEMPLATE_CHAIN_BROKEN, ex.getError());
    }

    @Test
    @DisplayName("Should throw ConstraintException when one branch in multi-branch tree is broken")
    void testMultiBranchBrokenThrows() {
        TemplateCreateRequestDTO validChild = new TemplateCreateRequestDTO(
                "TPL_VALID", "Valid Child", "UTILITY", "en", "Terminal child", null, null, null, null, Collections.emptyList()
        );
        TemplateCreateRequestDTO root = new TemplateCreateRequestDTO(
                "TPL_ROOT", "Root", "UTILITY", "en", "Options", null, null, null, null,
                List.of(
                        new TemplateButtonDTO("Valid", ButtonType.QUICK_REPLY, "p1", "TPL_VALID", false, 1),
                        new TemplateButtonDTO("Broken", ButtonType.QUICK_REPLY, "p2", "TPL_GHOST", false, 2)
                ),
                List.of(validChild)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(root));
        assertEquals(TemplateConstraintErrors.TEMPLATE_CHAIN_BROKEN, ex.getError());
    }
}
