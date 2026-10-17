package com.freelance.alerts.service;

import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.whatsapp.WhatsAppUtility;
import com.freelance.dispatcher.data.AlertDispatchRequest;
import com.freelance.dispatcher.data.AlertDispatchResult;
import com.freelance.dispatcher.impl.whatsapp.Msg91WhatsAppDispatcherService;
import com.freelance.event.model.EventPriority;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import com.freelance.manager.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class Msg91WhatsAppDispatcherServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        try {
            com.freelance.config.CacheRegistry.getInstance().init("com.freelance.fixtures");
        } catch (IllegalStateException ignored) {}
        WhatsAppUtility.resumeQueue();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
        WhatsAppUtility.resumeQueue();
        ConfigurationEngine.invalidateConfig(WebhookConfigKeys.MSG91_AUTH_KEY);
        ConfigurationEngine.invalidateConfig(WebhookConfigKeys.MSG91_INTEGRATED_NUMBER);
    }

    @Test
    @DisplayName("Should return WHATSAPP channel")
    void testGetChannel() {
        Msg91WhatsAppDispatcherService dispatcher = new Msg91WhatsAppDispatcherService();
        assertEquals(AlertChannel.WHATSAPP, dispatcher.getChannel());
    }

    @Test
    @DisplayName("Should perform mock dispatch when running with placeholder MSG91 authkey")
    void testMockDispatch() {
        Msg91WhatsAppDispatcherService dispatcher = new Msg91WhatsAppDispatcherService();
        AlertTemplateEntity template = new AlertTemplateEntity(
                UUID.randomUUID(),
                "membership_reminder",
                "Membership Reminder",
                "UTILITY",
                "en",
                "Hi {{user.name}}, your membership at {{gym.name}} expires on {{date}}.",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                "user.name,gym.name,date",
                null,
                "HIGH"
        );

        AlertDispatchRequest request = new AlertDispatchRequest(
                UUID.randomUUID(),
                template,
                AlertChannel.WHATSAPP,
                "919876543210",
                null,
                Map.of("user.name", "Rahul", "gym.name", "Iron Gym", "date", "15th Oct"),
                EventPriority.HIGH
        );

        AlertDispatchResult result = dispatcher.dispatch(request);
        assertTrue(result.isSuccess());
        assertNotNull(result.externalMessageId());
        assertTrue(result.externalMessageId().startsWith("msg91_req_"));
        assertNotNull(result.rawResponse());
        assertTrue(result.rawResponse().contains("mock mode"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when queue is paused")
    void testDispatchWhenQueuePaused() {
        WhatsAppUtility.pauseQueue();
        Msg91WhatsAppDispatcherService dispatcher = new Msg91WhatsAppDispatcherService();

        AlertDispatchRequest request = new AlertDispatchRequest(
                UUID.randomUUID(),
                null,
                AlertChannel.WHATSAPP,
                "919876543210",
                "Direct message",
                Map.of(),
                EventPriority.NORMAL
        );

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(request));
    }

    @Test
    @DisplayName("Should dispatch live template message successfully when valid HTTP 200 response received")
    void testLiveTemplateDispatchSuccess() {
        WhatsAppTemplateServiceTest.FakeHttpClient fakeClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                200, "{\"status\":\"success\",\"message\":\"Sent\",\"request_id\":\"msg91_live_12345\"}"
        );
        AlertMessageRepository msgRepo = new AlertMessageRepository();

        CacheManager.put("cache_SystemConfig", WebhookConfigKeys.MSG91_AUTH_KEY.getKey(), "live_authkey_1234567890abcdef");
        CacheManager.put("cache_SystemConfig", WebhookConfigKeys.MSG91_INTEGRATED_NUMBER.getKey(), "919876543210");

        Msg91WhatsAppDispatcherService dispatcher = new Msg91WhatsAppDispatcherService(fakeClient, msgRepo, null);

        AlertTemplateEntity template = new AlertTemplateEntity(
                UUID.randomUUID(),
                "welcome_msg",
                "Welcome",
                "UTILITY",
                "en",
                "Welcome {{name}} to {{gym}}!",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                false,
                "name,gym",
                null,
                "NORMAL"
        );
        template.setButtons(List.of(
                new AlertButtonEntity(UUID.randomUUID(), template, "Renew", ButtonType.URL, "https://gym.com/pay", 1)
        ));

        AlertDispatchRequest request = new AlertDispatchRequest(
                UUID.randomUUID(),
                template,
                AlertChannel.WHATSAPP,
                "919988776655",
                null,
                Map.of("name", "Alice", "gym", "Crossfit"),
                EventPriority.NORMAL
        );

        AlertDispatchResult result = dispatcher.dispatch(request);
        assertTrue(result.isSuccess());
        assertEquals("msg91_live_12345", result.externalMessageId());
    }

    @Test
    @DisplayName("Should return failure result when MSG91 returns 400 error")
    void testLiveDispatchFailure() {
        WhatsAppTemplateServiceTest.FakeHttpClient fakeClient = new WhatsAppTemplateServiceTest.FakeHttpClient(
                400, "{\"status\":\"error\",\"message\":\"Invalid Phone Number\"}"
        );
        AlertMessageRepository msgRepo = new AlertMessageRepository();

        CacheManager.put("cache_SystemConfig", WebhookConfigKeys.MSG91_AUTH_KEY.getKey(), "live_authkey_1234567890abcdef");
        CacheManager.put("cache_SystemConfig", WebhookConfigKeys.MSG91_INTEGRATED_NUMBER.getKey(), "919876543210");

        Msg91WhatsAppDispatcherService dispatcher = new Msg91WhatsAppDispatcherService(fakeClient, msgRepo, null);

        AlertDispatchRequest request = new AlertDispatchRequest(
                UUID.randomUUID(),
                null,
                AlertChannel.WHATSAPP,
                "invalid_phone",
                "Test content",
                Map.of(),
                EventPriority.NORMAL
        );

        AlertDispatchResult result = dispatcher.dispatch(request);
        assertTrue(result.isFailure());
        assertNotNull(result.errorMessage());
        assertTrue(result.rawResponse().contains("Invalid Phone Number"));
    }
}
