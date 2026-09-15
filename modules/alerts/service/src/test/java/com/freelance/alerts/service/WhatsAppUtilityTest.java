package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.whatsapp.WhatsAppUtility;
import com.freelance.event.model.EventPriority;
import com.freelance.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppUtilityTest {

    static class FakeChannelConfigRepo extends AlertChannelConfigRepository {
        final Map<AlertChannel, AlertChannelConfigEntity> storage = new HashMap<>();

        @Override
        public Optional<AlertChannelConfigEntity> findByChannel(AlertChannel channel) {
            return Optional.ofNullable(storage.get(channel));
        }

        @Override
        public boolean isChannelAvailable(AlertChannel channel) {
            AlertChannelConfigEntity config = storage.get(channel);
            if (config == null) return true;
            return config.isActive();
        }
    }

    private FakeChannelConfigRepo channelConfigRepo;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        WhatsAppUtility.resumeQueue();
        channelConfigRepo = new FakeChannelConfigRepo();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
        WhatsAppUtility.resumeQueue();
    }

    @Test
    @DisplayName("canSendMessage returns true when contact is active, opted-in, and channel is available")
    void testCanSendMessage_success() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                false,
                true,
                null
        );

        boolean canSend = WhatsAppUtility.canSendMessage(contact, EventPriority.NORMAL, channelConfigRepo);
        assertTrue(canSend, "Should be eligible when queue is active, user opted-in, and no human reply delay");
    }

    @Test
    @DisplayName("canSendMessage returns false when queue is paused")
    void testCanSendMessage_queuePaused() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                false,
                true,
                null
        );
        WhatsAppUtility.pauseQueue();

        boolean canSend = WhatsAppUtility.canSendMessage(contact, EventPriority.NORMAL, channelConfigRepo);
        assertFalse(canSend, "Should not be able to send when queue is paused");
    }

    @Test
    @DisplayName("canSendMessage returns false when channel is down in channel configuration")
    void testCanSendMessage_channelConfigDown() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                false,
                true,
                null
        );
        channelConfigRepo.storage.put(AlertChannel.WHATSAPP, new AlertChannelConfigEntity(AlertChannel.WHATSAPP, false, "Down for maintenance"));

        boolean canSend = WhatsAppUtility.canSendMessage(contact, EventPriority.NORMAL, channelConfigRepo);
        assertFalse(canSend, "Should not be able to send when channel is inactive in config");
    }

    @Test
    @DisplayName("canSendMessage returns false when contact is null")
    void testCanSendMessage_nullContact() {
        boolean canSend = WhatsAppUtility.canSendMessage(null, EventPriority.NORMAL, channelConfigRepo);
        assertFalse(canSend, "Should not be able to send when contact is null");
    }

    @Test
    @DisplayName("canSendMessage returns false when user is opted out")
    void testCanSendMessage_userOptedOut() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                true, // opted out
                true,
                null
        );

        boolean canSend = WhatsAppUtility.canSendMessage(contact, EventPriority.NORMAL, channelConfigRepo);
        assertFalse(canSend, "Should not be able to send when user is opted out");
    }

    @Test
    @DisplayName("canSendMessage returns false when contact is inactive")
    void testCanSendMessage_channelInactive() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                false,
                false, // inactive
                null
        );

        boolean canSend = WhatsAppUtility.canSendMessage(contact, EventPriority.NORMAL, channelConfigRepo);
        assertFalse(canSend, "Should not be able to send when contact is inactive");
    }

    @Test
    @DisplayName("canSendMessage delays normal priority alerts during active human session, but permits high priority")
    void testCanSendMessage_activeHumanSession() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                false,
                true,
                Instant.now().plusSeconds(3600) // session active for 1h
        );

        boolean canSendNormal = WhatsAppUtility.canSendMessage(contact, EventPriority.NORMAL, channelConfigRepo);
        assertFalse(canSendNormal, "Should delay normal priority alerts when in active human support session");

        // High priority bypasses human delay check
        boolean canSendHigh = WhatsAppUtility.canSendMessage(contact, EventPriority.HIGH, channelConfigRepo);
        assertTrue(canSendHigh, "High priority OTP/urgent alerts should bypass human support session");
    }

    @Test
    @DisplayName("canSendMessage allows normal priority alerts once human session expires")
    void testCanSendMessage_expiredHumanSession() {
        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(),
                userId,
                AlertChannel.WHATSAPP,
                "+1234567890",
                false,
                true,
                Instant.now().minusSeconds(60) // expired
        );

        boolean canSend = WhatsAppUtility.canSendMessage(contact, EventPriority.NORMAL, channelConfigRepo);
        assertTrue(canSend, "Should allow normal priority alerts once human session expires");
    }

    @Test
    @DisplayName("Should detect default opt-out and opt-in keywords correctly")
    void testDefaultKeywordDetection() {
        assertTrue(WhatsAppUtility.isOptOutKeyword("STOP"));
        assertTrue(WhatsAppUtility.isOptOutKeyword("stop"));
        assertTrue(WhatsAppUtility.isOptOutKeyword("  UNSUBSCRIBE!  "));
        assertTrue(WhatsAppUtility.isOptOutKeyword("OPT OUT"));
        assertTrue(WhatsAppUtility.isOptOutKeyword("REVOKE."));
        assertFalse(WhatsAppUtility.isOptOutKeyword("HELLO"));
        assertFalse(WhatsAppUtility.isOptOutKeyword(""));
        assertFalse(WhatsAppUtility.isOptOutKeyword(null));

        assertTrue(WhatsAppUtility.isOptInKeyword("START"));
        assertTrue(WhatsAppUtility.isOptInKeyword("start"));
        assertTrue(WhatsAppUtility.isOptInKeyword("  SUBSCRIBE, "));
        assertTrue(WhatsAppUtility.isOptInKeyword("OPT IN"));
        assertTrue(WhatsAppUtility.isOptInKeyword("RESUME"));
        assertFalse(WhatsAppUtility.isOptInKeyword("STOP"));
        assertFalse(WhatsAppUtility.isOptInKeyword(null));
    }

    @Test
    @DisplayName("Should detect dynamically configured opt-out and opt-in keywords from ConfigurationEngine")
    void testDynamicKeywordConfiguration() {
        try {
            com.freelance.manager.CacheManager.getInstance().init("com.freelance.fixtures");
        } catch (IllegalStateException ignored) {
            // already initialized
        }

        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.AlertConfigKeys.WHATSAPP_OPT_OUT_KEYWORDS.getKey(),
                "HALT, CEASE, BLOCK_ME"
        );
        com.freelance.manager.CacheManager.getInstance().put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.AlertConfigKeys.WHATSAPP_OPT_IN_KEYWORDS.getKey(),
                "UNBLOCK, ALLOW_ME, GO"
        );

        try {
            assertTrue(WhatsAppUtility.isOptOutKeyword("HALT"));
            assertTrue(WhatsAppUtility.isOptOutKeyword("cease"));
            assertTrue(WhatsAppUtility.isOptOutKeyword("BLOCK_ME."));
            assertFalse(WhatsAppUtility.isOptOutKeyword("STOP")); // Overridden by custom configuration

            assertTrue(WhatsAppUtility.isOptInKeyword("UNBLOCK"));
            assertTrue(WhatsAppUtility.isOptInKeyword("allow_me"));
            assertTrue(WhatsAppUtility.isOptInKeyword("GO!"));
            assertFalse(WhatsAppUtility.isOptInKeyword("START")); // Overridden by custom configuration
        } finally {
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.AlertConfigKeys.WHATSAPP_OPT_OUT_KEYWORDS);
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.AlertConfigKeys.WHATSAPP_OPT_IN_KEYWORDS);
        }

        // Returns back to defaults after invalidation
        assertTrue(WhatsAppUtility.isOptOutKeyword("STOP"));
        assertTrue(WhatsAppUtility.isOptInKeyword("START"));
    }
}
