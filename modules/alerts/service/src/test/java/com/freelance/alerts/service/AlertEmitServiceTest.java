package com.freelance.alerts.service;

import com.freelance.alerts.AlertEmitService;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.AlertEmitRequestDTO;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.event.model.EventPriority;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AlertEmitServiceTest {

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<UUID, AlertTemplateEntity> storage = new HashMap<>();

        @Override
        public AlertTemplateEntity get(UUID id) {
            return storage.get(id);
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            storage.put(entity.getId(), entity);
        }
    }

    private TransactionContext context;
    private FakeAlertTemplateRepository templateRepository;
    private com.freelance.fixtures.FakeAlertTemplateCategoryRepository categoryRepository;
    private AlertEmitService service;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        templateRepository = new FakeAlertTemplateRepository();
        categoryRepository = new com.freelance.fixtures.FakeAlertTemplateCategoryRepository();
        service = new AlertEmitService(templateRepository, categoryRepository);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should resolve priority based on template category from repository")
    void testResolvePriority() {
        assertEquals(EventPriority.HIGH, service.resolvePriority("AUTHENTICATION"));
        assertEquals(EventPriority.HIGH, service.resolvePriority("authentication"));
        assertEquals(EventPriority.LOW, service.resolvePriority("MARKETING"));
        assertEquals(EventPriority.LOW, service.resolvePriority("marketing"));
        assertEquals(EventPriority.NORMAL, service.resolvePriority("UTILITY"));
        assertEquals(EventPriority.NORMAL, service.resolvePriority("utility"));
        assertEquals(EventPriority.NORMAL, service.resolvePriority("UNREGISTERED_CATEGORY"));
        assertEquals(EventPriority.NORMAL, service.resolvePriority((String) null));
        assertEquals(EventPriority.NORMAL, service.resolvePriority("   "));
    }

    @Test
    @DisplayName("Should resolve priority dynamically from custom category in repository table")
    void testResolvePriorityFromRepository() {
        categoryRepository.save(new com.freelance.alerts.template.entity.AlertTemplateCategoryEntity(
                UUID.randomUUID(), "URGENT_NOTIF", "Urgent Notification", "Urgent alerts", "HIGH"
        ));
        categoryRepository.save(new com.freelance.alerts.template.entity.AlertTemplateCategoryEntity(
                UUID.randomUUID(), "BULK_PROMO", "Bulk Promo", "Bulk marketing", "LOW"
        ));

        assertEquals(EventPriority.HIGH, service.resolvePriority("URGENT_NOTIF"));
        assertEquals(EventPriority.LOW, service.resolvePriority("BULK_PROMO"));
    }

    @Test
    @DisplayName("Should resolve priority from template-level priority field and fallback to category default")
    void testResolvePriorityFromTemplate() {
        // 1. UTILITY template with explicit HIGH priority override (e.g. emergency notice)
        AlertTemplateEntity emergencyTemplate = new AlertTemplateEntity(
                UUID.randomUUID(), "EMERGENCY_CLOSURE", "Emergency Closure", "UTILITY", "en",
                "Gym is closed today due to weather", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED, false, null, null, "HIGH"
        );
        assertEquals(EventPriority.HIGH, service.resolvePriority(emergencyTemplate));

        // 2. AUTHENTICATION template with explicit LOW priority override
        AlertTemplateEntity lowAuthTemplate = new AlertTemplateEntity(
                UUID.randomUUID(), "LOW_AUTH", "Low Auth", "AUTHENTICATION", "en",
                "Non-urgent verification", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED, false, null, null, "LOW"
        );
        assertEquals(EventPriority.LOW, service.resolvePriority(lowAuthTemplate));

        // 3. UTILITY template with null priority -> falls back to category default (NORMAL)
        AlertTemplateEntity defaultUtilityTemplate = new AlertTemplateEntity(
                UUID.randomUUID(), "REGULAR_UPDATE", "Regular Update", "UTILITY", "en",
                "Standard update", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED, false, null, null, null
        );
        assertEquals(EventPriority.NORMAL, service.resolvePriority(defaultUtilityTemplate));

        // 4. AUTHENTICATION template with null priority -> falls back to category default (HIGH)
        AlertTemplateEntity defaultAuthTemplate = new AlertTemplateEntity(
                UUID.randomUUID(), "AUTH_OTP", "OTP", "AUTHENTICATION", "en",
                "Your OTP is {{AUTH.otp}}", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED, true, "AUTH.otp", null, null
        );
        assertEquals(EventPriority.HIGH, service.resolvePriority(defaultAuthTemplate));

        // 5. Null template check
        assertEquals(EventPriority.NORMAL, service.resolvePriority((AlertTemplateEntity) null));
    }

    @Test
    @DisplayName("Should enqueue batch alerts successfully for approved template")
    void testEmitBatchAlertSuccess() {
        UUID templateId = UUID.randomUUID();
        AlertTemplateEntity template = new AlertTemplateEntity(
                templateId, "AUTH_OTP", "OTP Verification", "AUTHENTICATION", "en",
                "Your OTP is {{AUTH.otp}}", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED, true, "AUTH.otp", null
        );
        templateRepository.persist(template);

        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        AlertEmitRequestDTO request = new AlertEmitRequestDTO(templateId, userIds);

        assertDoesNotThrow(() -> service.emitBatchAlert(request));
    }

    @Test
    @DisplayName("Should throw ConstraintException when template does not exist")
    void testEmitBatchAlertTemplateNotFound() {
        UUID templateId = UUID.randomUUID();
        AlertEmitRequestDTO request = new AlertEmitRequestDTO(templateId, List.of(UUID.randomUUID()));

        assertThrows(ConstraintException.class, () -> service.emitBatchAlert(request));
    }

    @Test
    @DisplayName("Should throw ConstraintException when template is not APPROVED")
    void testEmitBatchAlertTemplateNotApproved() {
        UUID templateId = UUID.randomUUID();
        AlertTemplateEntity template = new AlertTemplateEntity(
                templateId, "DRAFT_TPL", "Draft", "UTILITY", "en",
                "Draft content", null, null, AlertChannel.WHATSAPP,
                TemplateStatus.DRAFT, false, null, null
        );
        templateRepository.persist(template);

        AlertEmitRequestDTO request = new AlertEmitRequestDTO(templateId, List.of(UUID.randomUUID()));

        assertThrows(ConstraintException.class, () -> service.emitBatchAlert(request));
    }

    @Test
    @DisplayName("Should throw ValidationException when request has empty userIds")
    void testEmitBatchAlertValidationFailure() {
        AlertEmitRequestDTO request = new AlertEmitRequestDTO(UUID.randomUUID(), Collections.emptyList());

        assertThrows(ValidationException.class, () -> service.emitBatchAlert(request));
    }
}
