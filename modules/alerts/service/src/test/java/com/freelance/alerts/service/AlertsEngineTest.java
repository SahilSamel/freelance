package com.freelance.alerts.service;

import com.freelance.alerts.AlertsEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.event.model.EventPriority;
import com.freelance.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AlertsEngineTest {

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

    private FakeAlertTemplateRepository templateRepo;
    private AlertsEngine engine;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        templateRepo = new FakeAlertTemplateRepository();
        engine = new AlertsEngine(templateRepo);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should emit alert using templateId and look up channel")
    void testEmitAlert_byId() {
        UUID templateId = UUID.randomUUID();
        AlertTemplateEntity template = new AlertTemplateEntity(
                templateId,
                "wa_alert",
                "WA Alert",
                "AUTHENTICATION",
                "en",
                "Your OTP is {{otp}}",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                null,
                null
        );
        templateRepo.persist(template);

        // Does not throw
        assertDoesNotThrow(() -> engine.emitAlert(templateId, UUID.randomUUID()));
        assertDoesNotThrow(() -> engine.emitAlert(templateId, UUID.randomUUID(), EventPriority.HIGH));
    }

    @Test
    @DisplayName("Should emit alert using ITemplate object directly")
    void testEmitAlert_byITemplate() {
        ITemplate mockTemplate = new ITemplate() {
            @Override
            public String getTemplateKey() {
                return "TEST_TPL";
            }

            @Override
            public String getName() {
                return "Test Template";
            }

            @Override
            public String getCategory() {
                return "UTILITY";
            }

            @Override
            public String getLanguage() {
                return "en";
            }

            @Override
            public String getContent() {
                return "Content";
            }

            @Override
            public String getMediaUrl() {
                return null;
            }

            @Override
            public String getMediaType() {
                return null;
            }

            @Override
            public AlertChannel getChannel() {
                return AlertChannel.WHATSAPP;
            }

            @Override
            public Set<String> getRequiredAttributes() {
                return Collections.emptySet();
            }

            @Override
            public List<ITemplateButton> getButtons() {
                return Collections.emptyList();
            }
        };

        assertDoesNotThrow(() -> engine.emitAlert(mockTemplate, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should handle null arguments gracefully")
    void testEmitAlert_nullArguments() {
        assertDoesNotThrow(() -> engine.emitAlert((UUID) null, UUID.randomUUID()));
        assertDoesNotThrow(() -> engine.emitAlert(UUID.randomUUID(), null));
        assertDoesNotThrow(() -> engine.emitAlert((ITemplate) null, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Should emit alert using ITemplate with explicit priority")
    void testEmitAlert_byITemplateWithExplicitPriority() {
        ITemplate mockHighPriorityTemplate = new ITemplate() {
            @Override public String getTemplateKey() { return "URGENT_TPL"; }
            @Override public String getName() { return "Urgent Template"; }
            @Override public String getCategory() { return "UTILITY"; }
            @Override public String getPriority() { return "HIGH"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Emergency Closure Alert"; }
            @Override public String getMediaUrl() { return null; }
            @Override public String getMediaType() { return null; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public List<ITemplateButton> getButtons() { return Collections.emptyList(); }
        };

        assertDoesNotThrow(() -> engine.emitAlert(mockHighPriorityTemplate, UUID.randomUUID()));
    }
}
