package com.freelance.alerts.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.ConfigurationEngine;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertConsumer;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertErrorCode;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.attribute.IDomainAttributeResolver;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.dto.webhook.WhatsAppWebhookPayloadDTO;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.mock.MockWhatsAppHttpServer;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonReplyRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.templates.*;
import com.freelance.alerts.whatsapp.WhatsAppAlertProcessor;
import com.freelance.alerts.whatsapp.WhatsAppUtility;
import com.freelance.attribute.AttributeResolutionEngine;
import com.freelance.dispatcher.AlertDispatcherFactory;
import com.freelance.dispatcher.IAlertDispatcher;
import com.freelance.dispatcher.impl.whatsapp.Msg91WhatsAppDispatcherService;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.webhook.WebhookConfigKeys;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.template.seeder.TemplateSeeder;
import com.freelance.user.UserDTO;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;
import com.freelance.user.read.UserReadService;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("End-to-End WhatsApp Integration Test Suite (Real HTTP & Inbound Mock)")
class WhatsAppMockIntegrationTest {

    private static final String TEST_AUTH_KEY = "live_test_authkey_9876543210";
    private static final String TEST_INTEGRATED_NUMBER = "919876543210";
    private static final String TEST_CUSTOMER_PHONE = "919988776655";

    // ── In-Memory Repository Implementations ──────────────────────────────────

    static class TestUserContactRepository extends UserContactRepository {
        final Map<UUID, UserContactEntity> byId = new HashMap<>();
        final Map<String, UserContactEntity> byContact = new HashMap<>();
        final Map<String, UserContactEntity> byUserAndChannel = new HashMap<>();

        @Override
        public Optional<UserContactEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
            if (userId == null || channel == null) return Optional.empty();
            return Optional.ofNullable(byUserAndChannel.get(userId + ":" + channel));
        }

        @Override
        public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
            if (contactValue == null || channel == null) return Optional.empty();
            String key = contactValue.trim() + ":" + channel;
            if (byContact.containsKey(key)) return Optional.of(byContact.get(key));

            String alt = contactValue.startsWith("+") ? contactValue.substring(1) : "+" + contactValue;
            String altKey = alt + ":" + channel;
            return Optional.ofNullable(byContact.get(altKey));
        }

        @Override
        public void persist(UserContactEntity entity) {
            byId.put(entity.getId(), entity);
            byContact.put(entity.getContactValue() + ":" + entity.getChannel(), entity);
            byUserAndChannel.put(entity.getUserId() + ":" + entity.getChannel(), entity);
        }

        @Override
        public UserContactEntity merge(UserContactEntity entity) {
            persist(entity);
            return entity;
        }
    }

    static class TestAlertMessageRepository extends AlertMessageRepository {
        final Map<UUID, AlertMessageEntity> byId = new HashMap<>();
        final Map<String, AlertMessageEntity> byMetaId = new HashMap<>();
        final List<AlertMessageEntity> messages = new ArrayList<>();

        @Override
        public void persist(AlertMessageEntity entity) {
            byId.put(entity.getId(), entity);
            if (entity.getMetaMessageId() != null) {
                byMetaId.put(entity.getMetaMessageId(), entity);
            }
            messages.add(entity);
        }

        @Override
        public AlertMessageEntity merge(AlertMessageEntity entity) {
            persist(entity);
            return entity;
        }

        @Override
        public Optional<AlertMessageEntity> findByMetaMessageId(String metaMessageId) {
            if (metaMessageId == null) return Optional.empty();
            return Optional.ofNullable(byMetaId.get(metaMessageId));
        }

        @Override
        public Optional<AlertMessageEntity> findLatestByUserAndChannel(UUID userId, AlertChannel channel) {
            return messages.stream()
                    .filter(m -> userId.equals(m.getUserId()) && channel == m.getChannel())
                    .reduce((first, second) -> second);
        }
    }

    static class TestAlertChannelConfigRepository extends AlertChannelConfigRepository {
        @Override
        public Optional<AlertChannelConfigEntity> findByChannel(AlertChannel channel) {
            return Optional.of(new AlertChannelConfigEntity(channel, true, "Active channel"));
        }

        @Override
        public boolean isChannelAvailable(AlertChannel channel) {
            return true;
        }
    }

    static class TestAlertTemplateRepository extends AlertTemplateRepository {
        final Map<UUID, AlertTemplateEntity> byId = new HashMap<>();
        final Map<String, AlertTemplateEntity> byKey = new HashMap<>();
        final Map<String, AlertTemplateEntity> byMetaId = new HashMap<>();

        @Override
        public AlertTemplateEntity get(UUID id) {
            return byId.get(id);
        }

        @Override
        public Optional<AlertTemplateEntity> findByKey(String key) {
            if (key == null) return Optional.empty();
            return Optional.ofNullable(byKey.get(key));
        }

        @Override
        public Optional<AlertTemplateEntity> findByKeyIgnoreCase(String key) {
            if (key == null) return Optional.empty();
            return byKey.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(key))
                    .map(Map.Entry::getValue)
                    .findFirst();
        }

        @Override
        public Optional<AlertTemplateEntity> findByMetaTemplateId(String metaId) {
            if (metaId == null) return Optional.empty();
            return Optional.ofNullable(byMetaId.get(metaId));
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            byId.put(entity.getId(), entity);
            byKey.put(entity.getTemplateKey(), entity);
            if (entity.getMetaTemplateId() != null) {
                byMetaId.put(entity.getMetaTemplateId(), entity);
            }
        }

        @Override
        public AlertTemplateEntity merge(AlertTemplateEntity entity) {
            persist(entity);
            return entity;
        }
    }

    static class TestAlertButtonReplyRepository extends AlertButtonReplyRepository {
        final Map<String, AlertButtonReplyEntity> byPayload = new HashMap<>();

        @Override
        public Optional<AlertButtonReplyEntity> findByButtonPayload(String payload) {
            if (payload == null) return Optional.empty();
            return Optional.ofNullable(byPayload.get(payload));
        }
    }

    static class TestUserReadService extends UserReadService {
        final Set<UUID> validUsers = new HashSet<>();

        @Override
        public UserReadResponseDTO readUser(UserReadRequestDTO request) {
            if (request != null && request.id() != null && validUsers.contains(request.id())) {
                UserDTO dto = new UserDTO(
                        request.id(),
                        "aarav_sharma",
                        "password",
                        "Aarav",
                        "Sharma",
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
                return new UserReadResponseDTO(dto);
            }
            throw new RuntimeException("User not found");
        }
    }

    static class TestAlertsEngine extends com.freelance.alerts.AlertsEngine {
        private WhatsAppAlertProcessor processor;

        TestAlertsEngine(AlertTemplateRepository templateRepository) {
            super(templateRepository);
        }

        void setProcessor(WhatsAppAlertProcessor processor) {
            this.processor = processor;
        }

        @Override
        public void emitAlert(UUID templateId, UUID userId, EventPriority priority) {
            if (processor != null) {
                processor.processOutbound(EventMessage.of(
                        AlertConsumer.OUTBOUND_WHATSAPP,
                        new AlertEventPayloadDTO(templateId, userId),
                        priority != null ? priority : EventPriority.HIGH
                ));
            }
        }

        @Override
        public void emitAlert(UUID templateId, UUID userId) {
            emitAlert(templateId, userId, EventPriority.HIGH);
        }
    }

    // ── Test Setup & Infrastructure ───────────────────────────────────────────

    private MockWhatsAppHttpServer mockServer;
    private TransactionContext context;
    private UUID testUserId;
    private UserContactEntity testContact;

    private TestUserReadService userReadService;
    private TestUserContactRepository contactRepo;
    private TestAlertMessageRepository messageRepo;
    private TestAlertChannelConfigRepository channelConfigRepo;
    private TestAlertTemplateRepository templateRepo;
    private TestAlertButtonReplyRepository buttonReplyRepo;
    private TestAlertsEngine testAlertsEngine;
    private WhatsAppAlertProcessor processor;

    private List<ITemplate> codeTemplates;

    @BeforeEach
    void setUp() throws IOException {
        TestContextHelper.setUp();
        try {
            com.freelance.config.CacheRegistry.getInstance().init("com.freelance.fixtures");
        } catch (IllegalStateException ignored) {}

        mockServer = new MockWhatsAppHttpServer();
        mockServer.start();
        WhatsAppUtility.resumeQueue();

        testUserId = UUID.randomUUID();
        userReadService = new TestUserReadService();
        userReadService.validUsers.add(testUserId);

        contactRepo = new TestUserContactRepository();
        testContact = new UserContactEntity(UUID.randomUUID(), testUserId, AlertChannel.WHATSAPP, TEST_CUSTOMER_PHONE, false, true, null);
        contactRepo.persist(testContact);

        messageRepo = new TestAlertMessageRepository();
        channelConfigRepo = new TestAlertChannelConfigRepository();
        templateRepo = new TestAlertTemplateRepository();
        buttonReplyRepo = new TestAlertButtonReplyRepository();

        // Configure system configs to hit our mock HTTP server URL with real HTTP client calls
        CacheManager.put("cache_SystemConfig", WebhookConfigKeys.MSG91_AUTH_KEY.getKey(), TEST_AUTH_KEY);
        CacheManager.put("cache_SystemConfig", WebhookConfigKeys.MSG91_INTEGRATED_NUMBER.getKey(), TEST_INTEGRATED_NUMBER);
        CacheManager.put("cache_SystemConfig", WebhookConfigKeys.MSG91_API_BASE_URL.getKey(), mockServer.getBaseUrl());

        // Register Real Dispatcher hitting Mock Server URL
        Msg91WhatsAppDispatcherService realDispatcher = new Msg91WhatsAppDispatcherService(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                messageRepo,
                new ObjectMapper()
        );
        AlertDispatcherFactory dispatcherFactory = new AlertDispatcherFactory(realDispatcher);

        // Register All Domain Attribute Resolvers (USER, GYM, PLAN, PAYMENT, MEMBER)
        setupAttributeResolvers();

        // Seed All 13 Mandated Templates into Template Repository
        seedAllTemplates();

        // Create AlertsEngine forwarding directly to processor in-test
        testAlertsEngine = new TestAlertsEngine(templateRepo);

        processor = new WhatsAppAlertProcessor(
                userReadService,
                templateRepo,
                contactRepo,
                channelConfigRepo,
                messageRepo,
                buttonReplyRepo,
                dispatcherFactory,
                testAlertsEngine
        );

        testAlertsEngine.setProcessor(processor);
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop();
        }
        AttributeResolutionEngine.getInstance().resetForTesting();
        WhatsAppUtility.resumeQueue();
        TestContextHelper.tearDown();
        ConfigurationEngine.invalidateConfig(WebhookConfigKeys.MSG91_AUTH_KEY);
        ConfigurationEngine.invalidateConfig(WebhookConfigKeys.MSG91_INTEGRATED_NUMBER);
        ConfigurationEngine.invalidateConfig(WebhookConfigKeys.MSG91_API_BASE_URL);
    }

    private void seedAllTemplates() {
        codeTemplates = List.of(
                new ProspectWalkinRecapTemplate(),
                new CampaignSeasonalOfferTemplate(),
                new MemberWelcomeOnboardingTemplate(),
                new MemberRenewalConfirmationTemplate(),
                new PaymentReceiptInstantTemplate(),
                new PlanRenewalReminderTemplate(),
                new PaymentOverdueReminderTemplate(),
                new MemberLapsedWinbackTemplate(),
                new GoogleReviewRequestTemplate()
        );

        TemplateSeeder seeder = new TemplateSeeder(templateRepo);
        seeder.seed(codeTemplates);

        // Mark all templates APPROVED for dispatching in test environment
        // and register button replies into buttonReplyRepo for quick lookup
        for (ITemplate tpl : codeTemplates) {
            templateRepo.findByKey(tpl.getTemplateKey()).ifPresent(entity -> {
                entity.setStatus(TemplateStatus.APPROVED);
                if (entity.getButtons() != null) {
                    entity.getButtons().forEach(btn -> {
                        if (btn.getReply() != null && btn.getPayload() != null) {
                            buttonReplyRepo.byPayload.put(btn.getPayload(), btn.getReply());
                        }
                    });
                }
            });
        }
    }

    private void setupAttributeResolvers() {
        AttributeResolutionEngine.getInstance().resetForTesting();

        AttributeResolutionEngine.getInstance().register(new IDomainAttributeResolver() {
            @Override public String getDomain() { return "GYM"; }
            @Override public Set<String> getSupportedAttributes() {
                return Set.of("GYM.name", "GYM.timings", "GYM.support_phone", "GYM.address", "GYM.google_maps_url", "GYM.whatsapp_channel_url", "GYM.plan_catalogue");
            }
            @Override public String getAttributeDescription(String a) { return a; }
            @Override public Map<String, String> resolve(UUID u, Set<String> attrs) {
                return Map.of(
                        "GYM.name", "Iron Fitness Club",
                        "GYM.timings", "6:00 AM - 10:00 PM (Mon-Sat)",
                        "GYM.support_phone", "+919876543210",
                        "GYM.address", "101 Muscle Road, Bandra West, Mumbai",
                        "GYM.google_maps_url", "https://maps.google.com/?q=Iron+Fitness+Club",
                        "GYM.whatsapp_channel_url", "https://whatsapp.com/channel/ironfitness",
                        "GYM.plan_catalogue", "• 1 Month: ₹2,500\n• 3 Months: ₹6,500\n• 1 Year: ₹18,000"
                );
            }
        });

        AttributeResolutionEngine.getInstance().register(new IDomainAttributeResolver() {
            @Override public String getDomain() { return "USER"; }
            @Override public Set<String> getSupportedAttributes() {
                return Set.of("USER.first_name", "USER.name", "USER.email", "USER.phone");
            }
            @Override public String getAttributeDescription(String a) { return a; }
            @Override public Map<String, String> resolve(UUID u, Set<String> attrs) {
                return Map.of(
                        "USER.first_name", "Aarav",
                        "USER.name", "Aarav Sharma",
                        "USER.email", "aarav@example.com",
                        "USER.phone", TEST_CUSTOMER_PHONE
                );
            }
        });

        AttributeResolutionEngine.getInstance().register(new IDomainAttributeResolver() {
            @Override public String getDomain() { return "PLAN"; }
            @Override public Set<String> getSupportedAttributes() {
                return Set.of("PLAN.name", "PLAN.expiry_date", "PLAN.days_left");
            }
            @Override public String getAttributeDescription(String a) { return a; }
            @Override public Map<String, String> resolve(UUID u, Set<String> attrs) {
                return Map.of(
                        "PLAN.name", "Annual Gold Membership",
                        "PLAN.expiry_date", "31 Dec 2026",
                        "PLAN.days_left", "5"
                );
            }
        });

        AttributeResolutionEngine.getInstance().register(new IDomainAttributeResolver() {
            @Override public String getDomain() { return "PAYMENT"; }
            @Override public Set<String> getSupportedAttributes() {
                return Set.of("PAYMENT.amount", "PAYMENT.due_date", "PAYMENT.receipt_id", "PAYMENT.mode", "PAYMENT.paid_date", "PAYMENT.total_paid", "PAYMENT.pending_balance", "PAYMENT.next_due_summary");
            }
            @Override public String getAttributeDescription(String a) { return a; }
            @Override public Map<String, String> resolve(UUID u, Set<String> attrs) {
                return Map.of(
                        "PAYMENT.amount", "₹2,500",
                        "PAYMENT.due_date", "15 Nov 2026",
                        "PAYMENT.receipt_id", "REC-2026-001",
                        "PAYMENT.mode", "UPI",
                        "PAYMENT.paid_date", "12 Oct 2026",
                        "PAYMENT.total_paid", "₹15,000",
                        "PAYMENT.pending_balance", "₹2,500",
                        "PAYMENT.next_due_summary", "Next due: ₹2,500 on 15 Nov 2026"
                );
            }
        });

        AttributeResolutionEngine.getInstance().register(new IDomainAttributeResolver() {
            @Override public String getDomain() { return "MEMBER"; }
            @Override public Set<String> getSupportedAttributes() {
                return Set.of("MEMBER.plans_breakdown");
            }
            @Override public String getAttributeDescription(String a) { return a; }
            @Override public Map<String, String> resolve(UUID u, Set<String> attrs) {
                return Map.of("MEMBER.plans_breakdown", "• Annual Gold: Active (ends 31 Dec 2026, 5 days left)");
            }
        });
    }

    // ── Integration Tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("1. Outbound Dispatch of All 9 Templates - Hits Real HTTP URL on Mock Server")
    void testDispatchAll9Templates_RealHttpToMockServer() {
        assertEquals(9, codeTemplates.size(), "All 9 system templates must be loaded");

        // Ensure active interactive session window so all templates including >3 button templates can dispatch
        testContact.setHumanSessionExpiresAt(Instant.now().plus(Duration.ofHours(24)));
        contactRepo.merge(testContact);

        for (ITemplate tpl : codeTemplates) {
            mockServer.clear();

            AlertTemplateEntity entity = templateRepo.findByKey(tpl.getTemplateKey()).orElseThrow();
            AlertEventPayloadDTO payload = new AlertEventPayloadDTO(
                    entity.getId(),
                    testUserId,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Execute real outbound processing pipeline
            processor.processOutbound(EventMessage.of(AlertConsumer.OUTBOUND_WHATSAPP, payload, EventPriority.HIGH));

            // Verify real HTTP request hit the Mock WhatsApp Server
            assertEquals(1, mockServer.getRecordedMessages().size(),
                    "Mock server should have received exactly 1 HTTP POST for template: " + tpl.getTemplateKey());

            MockWhatsAppHttpServer.RecordedOutboundMessage recorded = mockServer.getLastMessage().orElseThrow();

            assertEquals(TEST_CUSTOMER_PHONE, recorded.to());
            assertEquals(tpl.getTemplateKey(), recorded.templateName());
            assertEquals(TEST_AUTH_KEY, recorded.authKey());

            // Verify body variables were resolved and populated
            assertFalse(recorded.bodyVariables().isEmpty(),
                    "Body variables must be resolved for template: " + tpl.getTemplateKey());

            // Verify recorded in database with SENT status and received wamid
            AlertMessageEntity dbMsg = messageRepo.findByMetaMessageId(recorded.id()).orElseThrow();
            assertEquals(AlertMessageStatus.SENT, dbMsg.getStatus());
            assertEquals(testUserId, dbMsg.getUserId());
            assertEquals(AlertDirection.OUTBOUND, dbMsg.getDirection());
        }
    }

    @Test
    @DisplayName("2. Inbound Delivery Status Receipts - Sent -> Delivered -> Read Progression")
    void testDeliveryStatusProgression_SentToDeliveredToRead() {
        AlertTemplateEntity entity = templateRepo.findByKey("member_welcome_onboarding").orElseThrow();
        AlertEventPayloadDTO payload = new AlertEventPayloadDTO(entity.getId(), testUserId);

        // Step 1: Outbound message dispatched -> initial status SENT
        processor.processOutbound(EventMessage.of(AlertConsumer.OUTBOUND_WHATSAPP, payload, EventPriority.HIGH));
        MockWhatsAppHttpServer.RecordedOutboundMessage recorded = mockServer.getLastMessage().orElseThrow();
        String wamid = recorded.id();

        AlertMessageEntity dbMsg = messageRepo.findByMetaMessageId(wamid).orElseThrow();
        assertEquals(AlertMessageStatus.SENT, dbMsg.getStatus());

        // Step 2: Inbound webhook delivers "delivered" status
        WhatsAppWebhookPayloadDTO deliveredWebhook = mockServer.createStatusWebhookPayload(wamid, TEST_CUSTOMER_PHONE, "delivered");
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, deliveredWebhook, EventPriority.HIGH));

        dbMsg = messageRepo.findByMetaMessageId(wamid).orElseThrow();
        assertEquals(AlertMessageStatus.DELIVERED, dbMsg.getStatus(), "Status must transition to DELIVERED");

        // Step 3: Inbound webhook delivers "read" status
        WhatsAppWebhookPayloadDTO readWebhook = mockServer.createStatusWebhookPayload(wamid, TEST_CUSTOMER_PHONE, "read");
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, readWebhook, EventPriority.HIGH));

        dbMsg = messageRepo.findByMetaMessageId(wamid).orElseThrow();
        assertEquals(AlertMessageStatus.READ, dbMsg.getStatus(), "Status must transition to READ");
    }

    @Test
    @DisplayName("3. Inbound Customer Freeform Text - Starts 24-Hour Service Session Without Automated Bot Messages")
    void testInboundCustomerText_StartsServiceSessionWithoutAutomatedMessages() {
        mockServer.clear();

        // Customer replies: "Hey, I need help with gym membership"
        WhatsAppWebhookPayloadDTO textWebhook = mockServer.createCustomerTextPayload(TEST_CUSTOMER_PHONE, "Hey, I need help with gym membership");
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, textWebhook, EventPriority.HIGH));

        // 1. Verify inbound message saved to chat timeline
        Optional<AlertMessageEntity> latestInbound = messageRepo.messages.stream()
                .filter(m -> m.getDirection() == AlertDirection.INBOUND)
                .findFirst();

        assertTrue(latestInbound.isPresent());
        assertEquals("Hey, I need help with gym membership", latestInbound.get().getContent());
        assertEquals("TEXT", latestInbound.get().getMessageType());

        // 2. Verify NO automated welcome menu or bot reply was sent to mock server
        assertEquals(0, mockServer.getRecordedMessages().size(), "Mock server must receive NO outbound automated messages");

        // 3. Verify 24-hour service session is active
        assertNotNull(testContact.getHumanSessionExpiresAt());
        assertTrue(testContact.isHumanSessionActive());
    }

    @Test
    @DisplayName("4. Inbound Button Click: Chat with Reception -> Extends 24-Hour Service Session")
    void testInboundButtonClick_ChatWithReception() {
        mockServer.clear();
        assertNull(testContact.getHumanSessionExpiresAt());

        // Customer clicks button "Chat with Reception" (payload: CHAT_WITH_RECEPTION)
        WhatsAppWebhookPayloadDTO btnWebhook = mockServer.createCustomerButtonPayload(
                TEST_CUSTOMER_PHONE,
                "CHAT_WITH_RECEPTION",
                "Chat with Reception"
        );
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, btnWebhook, EventPriority.HIGH));

        // Verify NO automated outbound message was dispatched
        assertEquals(0, mockServer.getRecordedMessages().size());

        // Verify 24-hour service session is active
        assertNotNull(testContact.getHumanSessionExpiresAt());
        assertTrue(testContact.isHumanSessionActive());
    }

    @Test
    @DisplayName("5. Inbound Button Click: Custom Payload -> Extends 24-Hour Service Session")
    void testInboundButtonClick_CustomPayload() {
        mockServer.clear();

        WhatsAppWebhookPayloadDTO btnWebhook = mockServer.createCustomerButtonPayload(
                TEST_CUSTOMER_PHONE,
                "CUSTOM_INTERACTIVE_PAYLOAD",
                "Help"
        );
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, btnWebhook, EventPriority.HIGH));

        assertEquals(0, mockServer.getRecordedMessages().size());
        assertNotNull(testContact.getHumanSessionExpiresAt());
        assertTrue(testContact.isHumanSessionActive());
    }

    @Test
    @DisplayName("6. Inbound Button Click: Stop Notifications -> Triggers Opt-Out Without Extending Session")
    void testInboundButtonClick_StopNotifications() {
        mockServer.clear();
        testContact.setHumanSessionExpiresAt(null);
        testContact.setOptedOut(false);

        WhatsAppWebhookPayloadDTO btnWebhook = mockServer.createCustomerButtonPayload(
                TEST_CUSTOMER_PHONE,
                "STOP",
                "Stop Notifications"
        );
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, btnWebhook, EventPriority.HIGH));

        assertEquals(0, mockServer.getRecordedMessages().size());
        assertTrue(testContact.isOptedOut(), "Contact must be marked optedOut = true");
        assertTrue(testContact.isActive(), "Contact must remain active = true on opt-out");
        assertNull(testContact.getHumanSessionExpiresAt(), "Session must not be extended on opt-out");
    }

    @Test
    @DisplayName("7. Inbound Message Follow-Up -> Overwrites Expiration with Rolling 24-Hour Window")
    void testInboundMessage_RollingWindowExtension() {
        mockServer.clear();
        Instant initialExpiry = Instant.now().plus(Duration.ofHours(5));
        testContact.setHumanSessionExpiresAt(initialExpiry);

        WhatsAppWebhookPayloadDTO textWebhook = mockServer.createCustomerTextPayload(TEST_CUSTOMER_PHONE, "Follow-up question");
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, textWebhook, EventPriority.HIGH));

        assertNotNull(testContact.getHumanSessionExpiresAt());
        assertTrue(testContact.getHumanSessionExpiresAt().isAfter(initialExpiry), "Session expiration must be overwritten with new rolling 24h window");
    }

    @Test
    @DisplayName("8. Inbound STOP Opt-Out - Enforces Marketing Restrictions while Permitting Utility")
    void testMarketingOptOutWithStop_EnforcesCategoryRestrictions() {
        mockServer.clear();
        assertFalse(testContact.isOptedOut());

        // Customer sends STOP
        WhatsAppWebhookPayloadDTO stopWebhook = mockServer.createCustomerTextPayload(TEST_CUSTOMER_PHONE, "STOP");
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, stopWebhook, EventPriority.HIGH));

        assertTrue(testContact.isOptedOut(), "Contact must be marked optedOut = true");

        // Attempt to send MARKETING template -> Must be blocked with ERR_USER_OPTED_OUT
        AlertTemplateEntity marketingTpl = templateRepo.findByKey("campaign_seasonal_offer").orElseThrow();
        processor.processOutbound(EventMessage.of(AlertConsumer.OUTBOUND_WHATSAPP, new AlertEventPayloadDTO(marketingTpl.getId(), testUserId), EventPriority.NORMAL));

        assertEquals(0, mockServer.getRecordedMessages().size(), "No HTTP request should be sent for marketing message when opted out");

        AlertMessageEntity failedMsg = messageRepo.messages.get(messageRepo.messages.size() - 1);
        assertEquals(AlertMessageStatus.FAILED, failedMsg.getStatus());
        assertEquals(AlertErrorCode.ERR_USER_OPTED_OUT, failedMsg.getErrorCode());

        // Attempt to send UTILITY template -> Must BYPASS opt-out and dispatch successfully
        AlertTemplateEntity utilityTpl = templateRepo.findByKey("payment_receipt_instant").orElseThrow();
        processor.processOutbound(EventMessage.of(AlertConsumer.OUTBOUND_WHATSAPP, new AlertEventPayloadDTO(utilityTpl.getId(), testUserId), EventPriority.HIGH));

        assertEquals(1, mockServer.getRecordedMessages().size(), "Utility messages must bypass marketing opt-out");
        assertEquals("payment_receipt_instant", mockServer.getLastMessage().orElseThrow().templateName());
    }

    @Test
    @DisplayName("9. Inbound START Opt-In - Re-enables Marketing Messages")
    void testMarketingOptInWithStart_ReenablesMarketingMessages() {
        // First opt out
        testContact.setOptedOut(true);

        // Customer sends START to opt back in
        WhatsAppWebhookPayloadDTO startWebhook = mockServer.createCustomerTextPayload(TEST_CUSTOMER_PHONE, "START");
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, startWebhook, EventPriority.HIGH));

        assertFalse(testContact.isOptedOut(), "Contact must now be opted back in");
        assertTrue(testContact.isActive());

        // Now dispatching MARKETING template should succeed
        mockServer.clear();
        AlertTemplateEntity marketingTpl = templateRepo.findByKey("campaign_seasonal_offer").orElseThrow();
        processor.processOutbound(EventMessage.of(AlertConsumer.OUTBOUND_WHATSAPP, new AlertEventPayloadDTO(marketingTpl.getId(), testUserId), EventPriority.NORMAL));

        assertEquals(1, mockServer.getRecordedMessages().size(), "Marketing template should dispatch after opt-in");
        assertEquals("campaign_seasonal_offer", mockServer.getLastMessage().orElseThrow().templateName());
    }

    @Test
    @DisplayName("10. Inbound Customer Image - Records Media & Extends Human Session")
    void testInboundCustomerImage_ScreenshotUpload() {
        mockServer.clear();

        // Customer sends payment screenshot image
        WhatsAppWebhookPayloadDTO imageWebhook = mockServer.createCustomerImagePayload(
                TEST_CUSTOMER_PHONE,
                "media_screenshot_999",
                "Here is my payment screenshot"
        );
        processor.processInbound(EventMessage.of(AlertConsumer.INBOUND_WHATSAPP, imageWebhook, EventPriority.HIGH));

        // Verify media message recorded
        Optional<AlertMessageEntity> inboundImg = messageRepo.messages.stream()
                .filter(m -> m.getDirection() == AlertDirection.INBOUND && "IMAGE".equals(m.getMessageType()))
                .findFirst();

        assertTrue(inboundImg.isPresent());
        assertEquals("media_screenshot_999", inboundImg.get().getMediaId());
        assertEquals("/alerts/media/media_screenshot_999", inboundImg.get().getMediaUrl());

        // Verify human session opened for receptionist verification
        assertNotNull(testContact.getHumanSessionExpiresAt());
        assertTrue(testContact.getHumanSessionExpiresAt().isAfter(Instant.now()));
    }
}
