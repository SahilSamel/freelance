package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WhatsAppDispatcherServiceTest {

    @Test
    @DisplayName("Should correctly replace dynamic placeholders in template content")
    void testRenderContent() {
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService();

        String templateText = "Hello {{user.username}}, your project {{project.title}} has a new update: {{update.summary}}!";
        Map<String, String> attributes = Map.of(
                "user.username", "Alice",
                "project.title", "E-Commerce App",
                "update.summary", "Milestone 1 Completed"
        );

        String rendered = dispatcher.renderContent(templateText, attributes);
        assertEquals("Hello Alice, your project E-Commerce App has a new update: Milestone 1 Completed!", rendered);
    }

    @Test
    @DisplayName("Should perform mock dispatch when running with placeholder token")
    void testMockDispatch() {
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService();
        AlertTemplateEntity template = new AlertTemplateEntity(
                UUID.randomUUID(),
                "test_welcome",
                "Welcome Message",
                "UTILITY",
                "en",
                "Welcome to Freelance Platform, {{user.username}}!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                "user.username",
                null
        );

        WhatsAppDispatcherService.DispatchResult result = dispatcher.dispatch(
                template,
                "+1234567890",
                Map.of("user.username", "Bob")
        );

        assertTrue(result.successful());
        assertNotNull(result.metaMessageId());
        assertTrue(result.metaMessageId().startsWith("wamid.HBgL"));
        assertEquals("Welcome to Freelance Platform, Bob!", result.renderedContent());
    }

    @Test
    @DisplayName("Should build valid Meta interactive list payload when buttons > 3")
    void testBuildInteractiveListPayload() {
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService();

        java.util.List<com.freelance.alerts.template.entity.AlertButtonEntity> buttons = java.util.List.of(
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), null, "Option 1", com.freelance.alerts.ButtonType.QUICK_REPLY, "p1", 1),
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), null, "Option 2", com.freelance.alerts.ButtonType.QUICK_REPLY, "p2", 2),
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), null, "Option 3", com.freelance.alerts.ButtonType.QUICK_REPLY, "p3", 3),
                new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), null, "Option 4", com.freelance.alerts.ButtonType.QUICK_REPLY, "p4", 4)
        );

        String json = dispatcher.buildInteractiveListPayload("+1234567890", "Choose a service:", buttons);
        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"interactive\""));
        assertTrue(json.contains("\"type\":\"list\""));
        assertTrue(json.contains("\"button\":\"Select Option\""));
        assertTrue(json.contains("\"title\":\"Option 1\""));
        assertTrue(json.contains("\"id\":\"p1\""));
        assertTrue(json.contains("\"title\":\"Option 4\""));
        assertTrue(json.contains("\"id\":\"p4\""));
    }
}