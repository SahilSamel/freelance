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
            com.freelance.config.CacheRegistry.getInstance().init("com.freelance.fixtures");
        } catch (IllegalStateException ignored) {
            // already initialized
        }

        com.freelance.manager.CacheManager.put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_OPT_OUT_KEYWORDS.getKey(),
                "HALT, CEASE, BLOCK_ME"
        );
        com.freelance.manager.CacheManager.put(
                "cache_SystemConfig",
                com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_OPT_IN_KEYWORDS.getKey(),
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
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_OPT_OUT_KEYWORDS);
            com.freelance.ConfigurationEngine.invalidateConfig(com.freelance.keys.alerts.webhook.WebhookConfigKeys.WHATSAPP_OPT_IN_KEYWORDS);
        }

        // Returns back to defaults after invalidation
        assertTrue(WhatsAppUtility.isOptOutKeyword("STOP"));
        assertTrue(WhatsAppUtility.isOptInKeyword("START"));
    }

    @Test
    @DisplayName("checkQueueNotPaused throws when paused and succeeds when not paused")
    void testCheckQueueNotPaused() {
        WhatsAppUtility.resumeQueue();
        assertDoesNotThrow(WhatsAppUtility::checkQueueNotPaused);

        WhatsAppUtility.pauseQueue();
        assertThrows(IllegalStateException.class, WhatsAppUtility::checkQueueNotPaused);
        WhatsAppUtility.resumeQueue();
    }

    @Test
    @DisplayName("canSendMessage convenience overloads behave consistently")
    void testCanSendMessageOverloads() {
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, "+12345", false, true, null
        );

        assertTrue(WhatsAppUtility.canSendMessage(contact));
        assertTrue(WhatsAppUtility.canSendMessage(contact, EventPriority.HIGH));
    }

    @Test
    @DisplayName("isInteractiveSessionActive evaluates humanSession and latest inbound message cutoff")
    void testIsInteractiveSessionActive() {
        assertFalse(WhatsAppUtility.isInteractiveSessionActive(null, null));

        UserContactEntity humanActive = new UserContactEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, "+12345", false, true, Instant.now().plusSeconds(3600)
        );
        assertTrue(WhatsAppUtility.isInteractiveSessionActive(humanActive, null));

        UserContactEntity standard = new UserContactEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, "+12345", false, true, null
        );

        // Repo with recent inbound message
        com.freelance.alerts.message.repository.AlertMessageRepository msgRepo = new com.freelance.alerts.message.repository.AlertMessageRepository() {
            @Override
            public Optional<com.freelance.alerts.message.entity.AlertMessageEntity> findLatestByUserAndChannel(UUID userId, AlertChannel channel) {
                com.freelance.alerts.message.entity.AlertMessageEntity msg = new com.freelance.alerts.message.entity.AlertMessageEntity();
                msg.setDirection(com.freelance.alerts.AlertDirection.INBOUND);
                msg.setTimestamp(Instant.now().minusSeconds(60));
                return Optional.of(msg);
            }
        };
        assertTrue(WhatsAppUtility.isInteractiveSessionActive(standard, msgRepo));

        // Repo with old inbound message (>24h)
        com.freelance.alerts.message.repository.AlertMessageRepository oldRepo = new com.freelance.alerts.message.repository.AlertMessageRepository() {
            @Override
            public Optional<com.freelance.alerts.message.entity.AlertMessageEntity> findLatestByUserAndChannel(UUID userId, AlertChannel channel) {
                com.freelance.alerts.message.entity.AlertMessageEntity msg = new com.freelance.alerts.message.entity.AlertMessageEntity();
                msg.setDirection(com.freelance.alerts.AlertDirection.INBOUND);
                msg.setTimestamp(Instant.now().minus(java.time.Duration.ofHours(25)));
                return Optional.of(msg);
            }
        };
        assertFalse(WhatsAppUtility.isInteractiveSessionActive(standard, oldRepo));
    }

    @Test
    @DisplayName("extendHumanSession updates expiration time and merges contact, extending rolling window")
    void testExtendHumanSession() {
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, "+12345", false, true, null
        );
        final boolean[] merged = {false};
        com.freelance.alerts.preference.repository.UserContactRepository contactRepo = new com.freelance.alerts.preference.repository.UserContactRepository() {
            @Override
            public UserContactEntity merge(UserContactEntity entity) {
                merged[0] = true;
                return entity;
            }
        };

        WhatsAppUtility.extendHumanSession(contact, contactRepo, java.time.Duration.ofHours(24));
        assertTrue(merged[0]);
        assertNotNull(contact.getHumanSessionExpiresAt());
        assertTrue(contact.isHumanSessionActive());

        // Subsequent call DOES extend and overwrite the timer (rolling window)
        merged[0] = false;
        java.time.Instant newBase = java.time.Instant.now().plusSeconds(3600);
        WhatsAppUtility.extendHumanSession(contact, contactRepo, java.time.Duration.ofHours(24), newBase);
        assertTrue(merged[0], "Should merge and update when extending session");
        assertEquals(newBase.plus(java.time.Duration.ofHours(24)), contact.getHumanSessionExpiresAt());

        // Null safe
        assertDoesNotThrow(() -> WhatsAppUtility.extendHumanSession(null, contactRepo, java.time.Duration.ofHours(1)));
        assertDoesNotThrow(() -> WhatsAppUtility.extendHumanSession(contact, null, java.time.Duration.ofHours(1)));
    }

    @Test
    @DisplayName("isUserValid checks user service and handles exceptions")
    void testIsUserValid() {
        assertFalse(WhatsAppUtility.isUserValid(null, null));
        assertFalse(WhatsAppUtility.isUserValid(UUID.randomUUID(), null));

        com.freelance.user.read.UserReadService mockService = new com.freelance.user.read.UserReadService() {
            @Override
            public com.freelance.user.read.UserReadResponseDTO readUser(com.freelance.user.read.UserReadRequestDTO req) {
                if (req.id() != null && req.id().getMostSignificantBits() == 1L) {
                    return new com.freelance.user.read.UserReadResponseDTO(
                            new com.freelance.user.UserDTO(req.id(), "valid_user", null, null, null)
                    );
                }
                throw new com.freelance.rest.app.service.exception.constraint.ConstraintException(null);
            }
        };

        assertTrue(WhatsAppUtility.isUserValid(new UUID(1L, 1L), mockService));
        assertFalse(WhatsAppUtility.isUserValid(new UUID(2L, 2L), mockService));
    }

    @Test
    @DisplayName("updateUserConsent updates existing and creates new contacts")
    void testUpdateUserConsent() {
        UUID userId = UUID.randomUUID();
        final Map<String, UserContactEntity> storage = new HashMap<>();

        com.freelance.alerts.preference.repository.UserContactRepository contactRepo = new com.freelance.alerts.preference.repository.UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByUserAndChannel(UUID uid, AlertChannel channel) {
                return storage.values().stream().filter(c -> uid.equals(c.getUserId())).findFirst();
            }
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contact, AlertChannel channel) {
                return Optional.ofNullable(storage.get(contact));
            }
            @Override
            public void persist(UserContactEntity entity) {
                storage.put(entity.getContactValue(), entity);
            }
            @Override
            public UserContactEntity merge(UserContactEntity entity) {
                storage.put(entity.getContactValue(), entity);
                return entity;
            }
        };

        // Create new contact
        WhatsAppUtility.updateUserConsent(userId, "+9112345", contactRepo, true, false);
        UserContactEntity created = storage.get("+9112345");
        assertNotNull(created);
        assertTrue(created.isOptedOut());
        assertTrue(created.isActive());

        // Update existing contact
        WhatsAppUtility.updateUserConsent(userId, "+9112345", contactRepo, false, true);
        UserContactEntity updated = storage.get("+9112345");
        assertFalse(updated.isOptedOut());
        assertTrue(updated.isActive());

        // Null safe
        assertDoesNotThrow(() -> WhatsAppUtility.updateUserConsent(null, null, null, false, false));
    }

    @Test
    @DisplayName("resolveMetaError and applyFailureAction handles various Meta error actions")
    void testMetaErrorResolutionAndApplication() {
        WhatsAppUtility.ErrorResolution res = WhatsAppUtility.resolveMetaError(131026, null, "Message undeliverable");
        assertNotNull(res);
        assertNotNull(res.normalizedCode());

        final Map<String, com.freelance.alerts.message.entity.AlertMessageEntity> messages = new HashMap<>();
        com.freelance.alerts.message.repository.AlertMessageRepository msgRepo = new com.freelance.alerts.message.repository.AlertMessageRepository() {
            @Override
            public Optional<com.freelance.alerts.message.entity.AlertMessageEntity> findByMetaMessageId(String metaId) {
                return Optional.ofNullable(messages.get(metaId));
            }
            @Override
            public com.freelance.alerts.message.entity.AlertMessageEntity merge(com.freelance.alerts.message.entity.AlertMessageEntity entity) {
                messages.put(entity.getMetaMessageId(), entity);
                return entity;
            }
        };

        UUID userId = UUID.randomUUID();
        com.freelance.alerts.message.entity.AlertMessageEntity msg = new com.freelance.alerts.message.entity.AlertMessageEntity();
        msg.setMetaMessageId("wamid_test_fail");
        msg.setUserId(userId);
        msg.setStatus(com.freelance.alerts.AlertMessageStatus.SENT);
        messages.put("wamid_test_fail", msg);

        // Apply failure action for undeliverable (DISABLE_USER_CHANNEL)
        WhatsAppUtility.applyFailureAction("wamid_test_fail", 131026, null, "Undeliverable", msgRepo, null, null);
        assertEquals(com.freelance.alerts.AlertMessageStatus.FAILED, msg.getStatus());

        // Null safe check
        assertDoesNotThrow(() -> WhatsAppUtility.applyFailureAction(null, 100, null, null, null, null, null));
    }

    @Test
    @DisplayName("Private constructor reflection test")
    void testConstructor() throws Exception {
        java.lang.reflect.Constructor<WhatsAppUtility> constructor = WhatsAppUtility.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        WhatsAppUtility instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    @DisplayName("resolveMetaError with unknown error code")
    void testResolveMetaErrorUnknown() {
        WhatsAppUtility.ErrorResolution res = WhatsAppUtility.resolveMetaError(999999, null, null);
        assertNotNull(res);
        assertTrue(res.normalizedCode().contains("ERR_META_UNKNOWN_999999"));
        assertNotNull(res.description());
    }

    @Test
    @DisplayName("applyFailureAction handles MARK_USER_OPTED_OUT, TRIGGER_HUMAN_INTERVENTION, CRITICAL_ADMIN_ALERT, and PAUSE_TEMPLATE")
    void testApplyFailureActionAllBranches() {
        final Map<String, com.freelance.alerts.message.entity.AlertMessageEntity> messages = new HashMap<>();
        final Map<UUID, UserContactEntity> contacts = new HashMap<>();
        final Map<String, com.freelance.alerts.template.entity.AlertTemplateEntity> templates = new HashMap<>();

        com.freelance.alerts.message.repository.AlertMessageRepository msgRepo = new com.freelance.alerts.message.repository.AlertMessageRepository() {
            @Override
            public Optional<com.freelance.alerts.message.entity.AlertMessageEntity> findByMetaMessageId(String metaId) {
                return Optional.ofNullable(messages.get(metaId));
            }
            @Override
            public com.freelance.alerts.message.entity.AlertMessageEntity merge(com.freelance.alerts.message.entity.AlertMessageEntity entity) {
                messages.put(entity.getMetaMessageId(), entity);
                return entity;
            }
        };

        com.freelance.alerts.preference.repository.UserContactRepository contactRepo = new com.freelance.alerts.preference.repository.UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByUserAndChannel(UUID uid, AlertChannel channel) {
                return Optional.ofNullable(contacts.get(uid));
            }
            @Override
            public UserContactEntity merge(UserContactEntity entity) {
                contacts.put(entity.getUserId(), entity);
                return entity;
            }
        };

        com.freelance.alerts.template.repository.AlertTemplateRepository tplRepo = new com.freelance.alerts.template.repository.AlertTemplateRepository() {
            @Override
            public com.freelance.alerts.template.entity.AlertTemplateEntity merge(com.freelance.alerts.template.entity.AlertTemplateEntity entity) {
                templates.put(entity.getTemplateKey(), entity);
                return entity;
            }
        };

        UUID userId = UUID.randomUUID();
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+12345", false, true, null
        );
        contacts.put(userId, contact);

        com.freelance.alerts.template.entity.AlertTemplateEntity tpl = new com.freelance.alerts.template.entity.AlertTemplateEntity();
        tpl.setTemplateKey("TPL_PAUSE_TEST");
        tpl.setStatus(com.freelance.alerts.TemplateStatus.APPROVED);

        // 1. MARK_USER_OPTED_OUT (131049)
        com.freelance.alerts.message.entity.AlertMessageEntity msg1 = new com.freelance.alerts.message.entity.AlertMessageEntity();
        msg1.setMetaMessageId("wamid_opt_out");
        msg1.setUserId(userId);
        messages.put("wamid_opt_out", msg1);

        WhatsAppUtility.applyFailureAction("wamid_opt_out", 131049, null, "User blocked business", msgRepo, contactRepo, tplRepo);
        assertTrue(contacts.get(userId).isOptedOut());

        // 2. TRIGGER_HUMAN_INTERVENTION (131047)
        com.freelance.alerts.message.entity.AlertMessageEntity msg2 = new com.freelance.alerts.message.entity.AlertMessageEntity();
        msg2.setMetaMessageId("wamid_human");
        msg2.setUserId(userId);
        messages.put("wamid_human", msg2);

        WhatsAppUtility.applyFailureAction("wamid_human", 131047, null, "24h window expired", msgRepo, contactRepo, tplRepo);
        assertTrue(contacts.get(userId).isHumanSessionActive());

        // 3. CRITICAL_ADMIN_ALERT (131031)
        com.freelance.alerts.message.entity.AlertMessageEntity msg3 = new com.freelance.alerts.message.entity.AlertMessageEntity();
        msg3.setMetaMessageId("wamid_critical");
        msg3.setUserId(userId);
        messages.put("wamid_critical", msg3);

        WhatsAppUtility.applyFailureAction("wamid_critical", 131031, null, "Account suspended", msgRepo, contactRepo, tplRepo);
        assertTrue(WhatsAppUtility.isQueuePaused());
        WhatsAppUtility.resumeQueue();

        // 4. PAUSE_TEMPLATE with template attached (132000)
        com.freelance.alerts.message.entity.AlertMessageEntity msg4 = new com.freelance.alerts.message.entity.AlertMessageEntity();
        msg4.setMetaMessageId("wamid_tpl_pause");
        msg4.setUserId(userId);
        msg4.setTemplate(tpl);
        messages.put("wamid_tpl_pause", msg4);

        WhatsAppUtility.applyFailureAction("wamid_tpl_pause", 132000, null, "Template paused", msgRepo, contactRepo, tplRepo);
        assertEquals(com.freelance.alerts.TemplateStatus.PAUSED, tpl.getStatus());

        // 5. PAUSE_TEMPLATE without template attached
        com.freelance.alerts.message.entity.AlertMessageEntity msg5 = new com.freelance.alerts.message.entity.AlertMessageEntity();
        msg5.setMetaMessageId("wamid_no_tpl");
        msg5.setUserId(userId);
        messages.put("wamid_no_tpl", msg5);

        assertDoesNotThrow(() -> WhatsAppUtility.applyFailureAction("wamid_no_tpl", 132000, null, "Template paused", msgRepo, contactRepo, null));

        // 6. Retriable error (130429) -> status set to PENDING
        com.freelance.alerts.message.entity.AlertMessageEntity msg6 = new com.freelance.alerts.message.entity.AlertMessageEntity();
        msg6.setMetaMessageId("wamid_retriable");
        msg6.setUserId(userId);
        messages.put("wamid_retriable", msg6);

        WhatsAppUtility.applyFailureAction("wamid_retriable", 130429, null, "Rate limit", msgRepo, contactRepo, tplRepo);
        assertEquals(com.freelance.alerts.AlertMessageStatus.PENDING, msg6.getStatus());
    }

    @Test
    @DisplayName("isUserValid handles ValidationException and generic Exceptions")
    void testIsUserValidExceptions() {
        com.freelance.user.read.UserReadService throwingService = new com.freelance.user.read.UserReadService() {
            @Override
            public com.freelance.user.read.UserReadResponseDTO readUser(com.freelance.user.read.UserReadRequestDTO req) {
                if (req.id() != null && req.id().getMostSignificantBits() == 100L) {
                    throw new com.freelance.rest.app.service.exception.validation.ValidationException();
                }
                throw new RuntimeException("Unexpected database failure");
            }
        };

        assertFalse(WhatsAppUtility.isUserValid(new UUID(100L, 1L), throwingService));
        assertFalse(WhatsAppUtility.isUserValid(new UUID(200L, 1L), throwingService));
    }

    @Test
    @DisplayName("isInteractiveSessionActive returns false when repository throws exception")
    void testIsInteractiveSessionActiveException() {
        UserContactEntity contact = new UserContactEntity(
                UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, "+12345", false, true, null
        );

        com.freelance.alerts.message.repository.AlertMessageRepository failingRepo = new com.freelance.alerts.message.repository.AlertMessageRepository() {
            @Override
            public Optional<com.freelance.alerts.message.entity.AlertMessageEntity> findLatestByUserAndChannel(UUID userId, AlertChannel channel) {
                throw new RuntimeException("DB down");
            }
        };

        assertFalse(WhatsAppUtility.isInteractiveSessionActive(contact, failingRepo));
    }

    @Test
    @DisplayName("parseAttributes handles null, blank, and comma-separated attribute strings")
    void testParseAttributes() {
        assertTrue(WhatsAppUtility.parseAttributes(null).isEmpty());
        assertTrue(WhatsAppUtility.parseAttributes("").isEmpty());
        assertTrue(WhatsAppUtility.parseAttributes("   ").isEmpty());

        Set<String> attrs = WhatsAppUtility.parseAttributes("USER.name, USER.id , USER.email");
        assertEquals(3, attrs.size());
        assertTrue(attrs.contains("USER.name"));
        assertTrue(attrs.contains("USER.id"));
        assertTrue(attrs.contains("USER.email"));
    }
}

