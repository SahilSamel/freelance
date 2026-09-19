package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.dispatcher.impl.whatsapp.WhatsAppDispatcherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WhatsAppDispatcherServiceTest {

    @BeforeEach
    void setUp() {
        com.freelance.fixtures.TestContextHelper.setUp();
        try {
            com.freelance.manager.CacheManager.getInstance().init("com.freelance.fixtures");
        } catch (IllegalStateException ignored) {}
        com.freelance.alerts.whatsapp.WhatsAppUtility.resumeQueue();
    }

    @AfterEach
    void tearDown() {
        com.freelance.fixtures.TestContextHelper.tearDown();
        com.freelance.alerts.whatsapp.WhatsAppUtility.resumeQueue();
    }

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

    @Test
    @DisplayName("Should execute live HTTP dispatch successfully with HTTP 200 response")
    void testLiveHttpDispatchSuccess() {
        WhatsAppTemplateServiceTest.FakeHttpClient fakeClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                200, "{\"messages\":[{\"id\":\"wamid.LIVE12345\"}]}"
        );
        com.freelance.alerts.message.repository.AlertMessageRepository msgRepo = new com.freelance.alerts.message.repository.AlertMessageRepository();
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService(fakeClient, msgRepo);

        // Put live token in cache
        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey(),
                "EAAB_LIVE_SECRET_TOKEN"
        );

        try {
            AlertTemplateEntity template = new AlertTemplateEntity(
                    UUID.randomUUID(), "tpl_live", "Live", "UTILITY", "en_US",
                    "Live content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
            );

            com.freelance.dispatcher.data.AlertDispatchRequest request = new com.freelance.dispatcher.data.AlertDispatchRequest(
                    UUID.randomUUID(), template, AlertChannel.WHATSAPP, "+919876543210",
                    "Live content", Map.of(), com.freelance.event.model.EventPriority.NORMAL
            );

            com.freelance.dispatcher.data.AlertDispatchResult result = dispatcher.dispatch(request);
            assertTrue(result.isSuccess());
            assertEquals("wamid.LIVE12345", result.externalMessageId());
        } finally {
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        }
    }

    @Test
    @DisplayName("Should handle live HTTP dispatch with interactive list (>3 buttons)")
    void testLiveHttpDispatchInteractiveList() {
        WhatsAppTemplateServiceTest.FakeHttpClient fakeClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                200, "{\"messages\":[{\"id\":\"wamid.LIST12345\"}]}"
        );
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService(fakeClient, new com.freelance.alerts.message.repository.AlertMessageRepository());

        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey(),
                "EAAB_LIVE_SECRET_TOKEN"
        );

        try {
            AlertTemplateEntity template = new AlertTemplateEntity(
                    UUID.randomUUID(), "tpl_interactive_live", "Live", "UTILITY", "en_US",
                    "Choose an option", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
            );
            template.getButtons().addAll(List.of(
                    new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), template, "O1", com.freelance.alerts.ButtonType.QUICK_REPLY, "p1", 1),
                    new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), template, "O2", com.freelance.alerts.ButtonType.QUICK_REPLY, "p2", 2),
                    new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), template, "O3", com.freelance.alerts.ButtonType.QUICK_REPLY, "p3", 3),
                    new com.freelance.alerts.template.entity.AlertButtonEntity(UUID.randomUUID(), template, "O4", com.freelance.alerts.ButtonType.QUICK_REPLY, "p4", 4)
            ));

            com.freelance.dispatcher.data.AlertDispatchRequest request = new com.freelance.dispatcher.data.AlertDispatchRequest(
                    UUID.randomUUID(), template, AlertChannel.WHATSAPP, "+919876543210",
                    "Choose an option", Map.of(), com.freelance.event.model.EventPriority.NORMAL
            );

            com.freelance.dispatcher.data.AlertDispatchResult result = dispatcher.dispatch(request);
            assertTrue(result.isSuccess());
            assertEquals("wamid.LIST12345", result.externalMessageId());
        } finally {
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        }
    }

    @Test
    @DisplayName("Should handle live HTTP dispatch with plain text (null template)")
    void testLiveHttpDispatchPlainText() {
        WhatsAppTemplateServiceTest.FakeHttpClient fakeClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                200, "{\"messages\":[{\"id\":\"wamid.TEXT12345\"}]}"
        );
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService(fakeClient, new com.freelance.alerts.message.repository.AlertMessageRepository());

        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey(),
                "EAAB_LIVE_SECRET_TOKEN"
        );

        try {
            com.freelance.dispatcher.data.AlertDispatchRequest request = new com.freelance.dispatcher.data.AlertDispatchRequest(
                    UUID.randomUUID(), null, AlertChannel.WHATSAPP, "+919876543210",
                    "Hello \"world\" with special chars \n and \t tabs", Map.of(), com.freelance.event.model.EventPriority.NORMAL
            );

            com.freelance.dispatcher.data.AlertDispatchResult result = dispatcher.dispatch(request);
            assertTrue(result.isSuccess());
            assertEquals("wamid.TEXT12345", result.externalMessageId());
        } finally {
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        }
    }

    @Test
    @DisplayName("Should return failure on HTTP 400 or network exception from Meta API")
    void testLiveHttpDispatchFailureAndException() {
        WhatsAppTemplateServiceTest.FakeHttpClient errorClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                400, "{\"error\":{\"message\":\"Invalid phone number\"}}"
        );
        WhatsAppDispatcherService errorDispatcher = new WhatsAppDispatcherService(errorClient, new com.freelance.alerts.message.repository.AlertMessageRepository());

        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey(),
                "EAAB_LIVE_SECRET_TOKEN"
        );

        try {
            com.freelance.dispatcher.data.AlertDispatchRequest request = new com.freelance.dispatcher.data.AlertDispatchRequest(
                    UUID.randomUUID(), null, AlertChannel.WHATSAPP, "+919876543210",
                    "Test content", Map.of(), com.freelance.event.model.EventPriority.NORMAL
            );

            com.freelance.dispatcher.data.AlertDispatchResult errorResult = errorDispatcher.dispatch(request);
            assertTrue(errorResult.isFailure());
            assertEquals("ERR_DISPATCH_FAILED", errorResult.errorCode());

            // Network exception
            WhatsAppTemplateServiceTest.FakeHttpClient netErrClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                    new java.io.IOException("Socket timeout")
            );
            WhatsAppDispatcherService netDispatcher = new WhatsAppDispatcherService(netErrClient, new com.freelance.alerts.message.repository.AlertMessageRepository());
            com.freelance.dispatcher.data.AlertDispatchResult netResult = netDispatcher.dispatch(request);
            assertTrue(netResult.isFailure());
        } finally {
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        }
    }

    @Test
    @DisplayName("Should throw IllegalStateException when dispatching while queue is paused")
    void testDispatchWhenQueuePausedThrows() {
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService();
        com.freelance.alerts.whatsapp.WhatsAppUtility.pauseQueue();

        try {
            com.freelance.dispatcher.data.AlertDispatchRequest request = new com.freelance.dispatcher.data.AlertDispatchRequest(
                    UUID.randomUUID(), null, AlertChannel.WHATSAPP, "+12345", "Msg", Map.of(), com.freelance.event.model.EventPriority.NORMAL
            );
            assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(request));
        } finally {
            com.freelance.alerts.whatsapp.WhatsAppUtility.resumeQueue();
        }
    }

    @Test
    @DisplayName("Constructors and getChannel")
    void testConstructorsAndChannel() {
        WhatsAppDispatcherService d1 = new WhatsAppDispatcherService(new com.freelance.alerts.message.repository.AlertMessageRepository());
        assertEquals(AlertChannel.WHATSAPP, d1.getChannel());
    }

    @Test
    @DisplayName("Should build interactive list payload with null buttons and default body text")
    void testBuildInteractiveListWithNullButtons() {
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService();
        String json = dispatcher.buildInteractiveListPayload("+12345", null, null);
        assertNotNull(json);
        assertTrue(json.contains("Please select an option:"));
        assertTrue(json.contains("\"rows\":[]"));
    }

    @Test
    @DisplayName("Should extract fallback wamid when response JSON lacks id field")
    void testExtractWamidFallback() {
        WhatsAppTemplateServiceTest.FakeHttpClient fakeClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                200, "{\"status\":\"ok\"}"
        );
        WhatsAppDispatcherService dispatcher = new WhatsAppDispatcherService(fakeClient, new com.freelance.alerts.message.repository.AlertMessageRepository());

        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN.getKey(),
                "EAAB_LIVE_SECRET_TOKEN"
        );

        try {
            com.freelance.dispatcher.data.AlertDispatchRequest request = new com.freelance.dispatcher.data.AlertDispatchRequest(
                    UUID.randomUUID(), null, AlertChannel.WHATSAPP, "+919876543210",
                    "Hello \b \f \r test", Map.of(), com.freelance.event.model.EventPriority.NORMAL
            );

            com.freelance.dispatcher.data.AlertDispatchResult result = dispatcher.dispatch(request);
            assertTrue(result.isSuccess());
            assertNotNull(result.externalMessageId());
            assertTrue(result.externalMessageId().startsWith("wamid.HBgL"));
        } finally {
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_ACCESS_TOKEN);
        }
    }
}