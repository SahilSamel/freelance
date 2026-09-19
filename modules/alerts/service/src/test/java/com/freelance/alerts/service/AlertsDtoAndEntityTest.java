package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.dto.ChatMessageDTO;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.webhook.WebhookUserPreferenceDTO;
import com.freelance.alerts.preference.entity.UserContactEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Alerts DTOs & Entities Comprehensive Unit Tests")
class AlertsDtoAndEntityTest {

    @Test
    @DisplayName("ChatMessageDTO constructors and record getters")
    void testChatMessageDTO() {
        UUID msgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        ChatMessageDTO dto1 = new ChatMessageDTO(
                msgId, userId, AlertChannel.WHATSAPP, AlertDirection.INBOUND,
                "TEXT", "Hello", AlertMessageStatus.DELIVERED, now
        );
        assertEquals(msgId, dto1.id());
        assertEquals(userId, dto1.userId());
        assertEquals(AlertChannel.WHATSAPP, dto1.channel());
        assertEquals(AlertDirection.INBOUND, dto1.direction());
        assertEquals("TEXT", dto1.messageType());
        assertEquals("Hello", dto1.content());
        assertNull(dto1.buttons());
        assertEquals(AlertMessageStatus.DELIVERED, dto1.status());
        assertEquals(now, dto1.timestamp());
        assertNull(dto1.sequenceNumber());

        ChatMessageDTO dto2 = new ChatMessageDTO(
                msgId, userId, AlertChannel.WHATSAPP, AlertDirection.OUTBOUND,
                "TEMPLATE", "Menu", List.of(), AlertMessageStatus.SENT, now
        );
        assertNotNull(dto2.buttons());

        ChatMessageDTO dto3 = new ChatMessageDTO(
                10, msgId, userId, AlertChannel.WHATSAPP, AlertDirection.INBOUND,
                "TEXT", "Hello 10", AlertMessageStatus.DELIVERED, now
        );
        assertEquals(10, dto3.sequenceNumber());
    }

    @Test
    @DisplayName("WebhookUserPreferenceDTO resolveWaId and isOptOut logic")
    void testWebhookUserPreferenceDTO() {
        WebhookUserPreferenceDTO dto1 = new WebhookUserPreferenceDTO("12345", "+12345", "MARKETING", "OPT_OUT", null, null, null);
        assertEquals("12345", dto1.resolveWaId());
        assertTrue(dto1.isOptOut());

        WebhookUserPreferenceDTO dto2 = new WebhookUserPreferenceDTO("", "+12345", "MARKETING", null, "STOP", null, null);
        assertEquals("+12345", dto2.resolveWaId());
        assertTrue(dto2.isOptOut());

        WebhookUserPreferenceDTO dto3 = new WebhookUserPreferenceDTO(null, "+12345", "MARKETING", null, null, "BLOCK", null);
        assertEquals("+12345", dto3.resolveWaId());
        assertTrue(dto3.isOptOut());

        WebhookUserPreferenceDTO dto4 = new WebhookUserPreferenceDTO("12345", null, "MARKETING", null, null, null, null);
        assertTrue(dto4.isOptOut()); // default when all are null

        WebhookUserPreferenceDTO dto5 = new WebhookUserPreferenceDTO("12345", null, "MARKETING", "OPT_IN", null, "ACTIVE", null);
        assertFalse(dto5.isOptOut());
    }

    @Test
    @DisplayName("UserContactEntity constructors, getters, setters, and isHumanSessionActive")
    void testUserContactEntity() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        UserContactEntity entity1 = new UserContactEntity();
        entity1.setId(id);
        entity1.setUserId(userId);
        entity1.setChannel(AlertChannel.WHATSAPP);
        entity1.setContactValue("+1234567890");
        entity1.setOptedOut(false);
        entity1.setActive(true);

        assertEquals(id, entity1.getId());
        assertEquals(userId, entity1.getUserId());
        assertEquals(AlertChannel.WHATSAPP, entity1.getChannel());
        assertEquals("+1234567890", entity1.getContactValue());
        assertFalse(entity1.isOptedOut());
        assertTrue(entity1.isActive());
        assertFalse(entity1.isHumanSessionActive());

        // Constructor 4-arg
        UserContactEntity entity2 = new UserContactEntity(id, userId, AlertChannel.WHATSAPP, "+9876543210");
        assertEquals("+9876543210", entity2.getContactValue());

        // Constructor 6-arg
        UserContactEntity entity3 = new UserContactEntity(id, userId, AlertChannel.WHATSAPP, "+9876543210", true, false);
        assertTrue(entity3.isOptedOut());
        assertFalse(entity3.isActive());

        // Future human session
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        entity1.setHumanSessionExpiresAt(future);
        assertEquals(future, entity1.getHumanSessionExpiresAt());
        assertTrue(entity1.isHumanSessionActive());

        // Past human session
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        entity1.setHumanSessionExpiresAt(past);
        assertFalse(entity1.isHumanSessionActive());
    }
}
