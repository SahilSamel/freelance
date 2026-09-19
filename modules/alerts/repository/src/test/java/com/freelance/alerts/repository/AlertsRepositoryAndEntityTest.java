package com.freelance.alerts.repository;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.channel.entity.AlertChannelConfigEntity;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertButtonReplyRepository;
import com.freelance.alerts.template.repository.AlertButtonRepository;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Alerts Repositories and Entities Unit Tests")
class AlertsRepositoryAndEntityTest {

    private String lastNamedQuery;
    private final Map<String, Object> lastParams = new HashMap<>();
    private boolean flushCalled = false;

    @BeforeEach
    void setUp() {
        lastNamedQuery = null;
        lastParams.clear();
        flushCalled = false;

        EntityTransaction mockTx = (EntityTransaction) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    if ("isActive".equals(method.getName())) return true;
                    return null;
                }
        );

        EntityManager mockEm = (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTransaction": return mockTx;
                        case "createNamedQuery":
                            lastNamedQuery = (String) args[0];
                            return createMockQuery();
                        case "flush":
                            flushCalled = true;
                            return null;
                        case "find":
                            if (AlertChannelConfigEntity.class.equals(args[0])) {
                                return new AlertChannelConfigEntity(AlertChannel.WHATSAPP, true, null);
                            }
                            return null;
                        case "close": return null;
                        default: return null;
                    }
                }
        );

        EntityManagerFactory mockEmf = (EntityManagerFactory) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("createEntityManager".equals(method.getName())) return mockEm;
                    return null;
                }
        );

        Transaction.init(mockEmf);
    }

    @SuppressWarnings("unchecked")
    private Object createMockQuery() {
        return Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TypedQuery.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setParameter":
                            lastParams.put((String) args[0], args[1]);
                            return proxy;
                        case "setMaxResults":
                        case "setFirstResult":
                            return proxy;
                        case "getResultList":
                            if ("AlertMessage.findByUserId".equals(lastNamedQuery) || "AlertMessage.findByUserAndChannel".equals(lastNamedQuery)) {
                                return List.of(new AlertMessageEntity(UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, AlertDirection.OUTBOUND, "TEMPLATE", "Hi", null, AlertMessageStatus.SENT, null, null, Instant.now()));
                            }
                            if ("AlertTemplate.findAll".equals(lastNamedQuery) || "AlertTemplate.findByStatus".equals(lastNamedQuery)) {
                                return List.of(new AlertTemplateEntity(UUID.randomUUID(), "TPL_KEY", "Name", "UTILITY", "en", "Content", null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, null));
                            }
                            if ("UserContact.findByUserId".equals(lastNamedQuery)) {
                                return List.of(new UserContactEntity(UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, "+1234567890"));
                            }
                            if ("AlertTemplateCategory.findAll".equals(lastNamedQuery)) {
                                return List.of(new AlertTemplateCategoryEntity(UUID.randomUUID(), "CAT_KEY", "Cat Name", "Desc", "HIGH"));
                            }
                            return Collections.emptyList();
                        default: return null;
                    }
                }
        );
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("AlertMessageRepository query methods")
    void testAlertMessageRepository() {
        try (var tx = Transaction.start()) {
            AlertMessageRepository repo = new AlertMessageRepository();

            assertTrue(repo.findByUserId(null).isEmpty());
            UUID userId = UUID.randomUUID();
            assertEquals(1, repo.findByUserId(userId).size());
            assertEquals("AlertMessage.findByUserId", lastNamedQuery);

            assertTrue(repo.findByUserAndChannel(null, AlertChannel.WHATSAPP).isEmpty());
            assertTrue(repo.findByUserAndChannel(userId, null).isEmpty());
            assertEquals(1, repo.findByUserAndChannel(userId, AlertChannel.WHATSAPP).size());

            assertTrue(repo.findByMetaMessageId(null).isEmpty());
            assertTrue(repo.findByMetaMessageId("   ").isEmpty());
            assertNotNull(repo.findByMetaMessageId("wamid.123"));

            assertTrue(repo.findLatestByUserId(null).isEmpty());
            assertNotNull(repo.findLatestByUserId(userId));

            assertTrue(repo.findLatestByUserAndChannel(null, AlertChannel.WHATSAPP).isEmpty());
            assertTrue(repo.findLatestByUserAndChannel(userId, null).isEmpty());
            assertNotNull(repo.findLatestByUserAndChannel(userId, AlertChannel.WHATSAPP));
        }
    }

    @Test
    @DisplayName("AlertTemplateRepository query methods")
    void testAlertTemplateRepository() {
        try (var tx = Transaction.start()) {
            AlertTemplateRepository repo = new AlertTemplateRepository();

            assertTrue(repo.findByKey(null).isEmpty());
            assertTrue(repo.findByKey(" ").isEmpty());
            assertNotNull(repo.findByKey("TPL_SAMPLE"));

            assertEquals(1, repo.findAllTemplates().size());

            assertEquals(1, repo.findByStatus(null).size());
            assertEquals(1, repo.findByStatus(TemplateStatus.APPROVED).size());

            assertTrue(repo.findByKeyIgnoreCase(null).isEmpty());
            assertTrue(repo.findByKeyIgnoreCase("").isEmpty());
            assertNotNull(repo.findByKeyIgnoreCase("tpl_sample"));

            assertTrue(repo.findByMetaTemplateId(null).isEmpty());
            assertTrue(repo.findByMetaTemplateId("  ").isEmpty());
            assertNotNull(repo.findByMetaTemplateId("meta_123"));

            repo.flush();
            assertTrue(flushCalled);
        }
    }

    @Test
    @DisplayName("AlertChannelConfigRepository methods")
    void testAlertChannelConfigRepository() {
        try (var tx = Transaction.start()) {
            AlertChannelConfigRepository repo = new AlertChannelConfigRepository();

            assertTrue(repo.findByChannel(null).isEmpty());
            assertTrue(repo.findByChannel(AlertChannel.WHATSAPP).isPresent());

            assertFalse(repo.isChannelAvailable(null));
            assertTrue(repo.isChannelAvailable(AlertChannel.WHATSAPP));
        }
    }

    @Test
    @DisplayName("UserContactRepository methods")
    void testUserContactRepository() {
        try (var tx = Transaction.start()) {
            UserContactRepository repo = new UserContactRepository();

            assertTrue(repo.findByUserAndChannel(null, AlertChannel.WHATSAPP).isEmpty());
            assertTrue(repo.findByUserAndChannel(UUID.randomUUID(), null).isEmpty());
            assertNotNull(repo.findByUserAndChannel(UUID.randomUUID(), AlertChannel.WHATSAPP));

            assertTrue(repo.findByContactAndChannel(null, AlertChannel.WHATSAPP).isEmpty());
            assertTrue(repo.findByContactAndChannel("+12345", null).isEmpty());
            assertNotNull(repo.findByContactAndChannel("+1234567890", AlertChannel.WHATSAPP));
            assertNotNull(repo.findByContactAndChannel("1234567890", AlertChannel.WHATSAPP));

            assertTrue(repo.findByUserId(null).isEmpty());
            assertEquals(1, repo.findByUserId(UUID.randomUUID()).size());
        }
    }

    @Test
    @DisplayName("AlertButtonRepository & AlertButtonReplyRepository methods")
    void testButtonAndReplyRepositories() {
        try (var tx = Transaction.start()) {
            AlertButtonRepository btnRepo = new AlertButtonRepository();
            assertTrue(btnRepo.findByPayload(null).isEmpty());
            assertTrue(btnRepo.findByPayload("   ").isEmpty());
            assertNotNull(btnRepo.findByPayload("payload_123"));

            AlertButtonReplyRepository replyRepo = new AlertButtonReplyRepository();
            assertTrue(replyRepo.findByButtonPayload(null).isEmpty());
            assertTrue(replyRepo.findByButtonPayload("").isEmpty());
            assertNotNull(replyRepo.findByButtonPayload("payload_123"));

            AlertTemplateCategoryRepository catRepo = new AlertTemplateCategoryRepository();
            assertTrue(catRepo.findByKey(null).isEmpty());
            assertTrue(catRepo.findByKey(" ").isEmpty());
            assertNotNull(catRepo.findByKey("UTILITY"));
            assertEquals(1, catRepo.findAllCategories().size());
        }
    }

    @Test
    @DisplayName("Entities getters and setters test")
    void testAllEntities() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        // AlertMessageEntity
        AlertMessageEntity msg = new AlertMessageEntity();
        msg.setId(id);
        msg.setUserId(userId);
        msg.setChannel(AlertChannel.WHATSAPP);
        msg.setDirection(AlertDirection.OUTBOUND);
        msg.setMessageType("TEMPLATE");
        msg.setContent("Test Content");
        msg.setMetaMessageId("wamid.1");
        msg.setStatus(AlertMessageStatus.SENT);
        msg.setErrorCode("ERR_NONE");
        msg.setRawErrorDetails("No error");
        msg.setTimestamp(now);

        assertEquals(id, msg.getId());
        assertEquals(userId, msg.getUserId());
        assertEquals(AlertChannel.WHATSAPP, msg.getChannel());
        assertEquals(AlertDirection.OUTBOUND, msg.getDirection());
        assertEquals("TEMPLATE", msg.getMessageType());
        assertEquals("Test Content", msg.getContent());
        assertEquals("wamid.1", msg.getMetaMessageId());
        assertEquals(AlertMessageStatus.SENT, msg.getStatus());
        assertEquals("ERR_NONE", msg.getErrorCode());
        assertEquals("No error", msg.getRawErrorDetails());
        assertEquals(now, msg.getTimestamp());

        // AlertTemplateEntity
        AlertTemplateEntity tpl = new AlertTemplateEntity();
        tpl.setId(id);
        tpl.setTemplateKey("TPL_KEY");
        tpl.setName("Name");
        tpl.setCategory("MARKETING");
        tpl.setLanguage("en_US");
        tpl.setContent("Body {{name}}");
        tpl.setMediaUrl("http://image.png");
        tpl.setMediaType("IMAGE");
        tpl.setChannel(AlertChannel.WHATSAPP);
        tpl.setStatus(TemplateStatus.APPROVED);
        tpl.setSystem(true);
        tpl.setRequiredAttributes("name");
        tpl.setMetaTemplateId("meta_tpl_1");
        tpl.setPriority("HIGH");
        tpl.setButtons(new ArrayList<>());

        assertEquals(id, tpl.getId());
        assertEquals("TPL_KEY", tpl.getTemplateKey());
        assertEquals("Name", tpl.getName());
        assertEquals("MARKETING", tpl.getCategory());
        assertEquals("en_US", tpl.getLanguage());
        assertEquals("Body {{name}}", tpl.getContent());
        assertEquals("http://image.png", tpl.getMediaUrl());
        assertEquals("IMAGE", tpl.getMediaType());
        assertEquals(AlertChannel.WHATSAPP, tpl.getChannel());
        assertEquals(TemplateStatus.APPROVED, tpl.getStatus());
        assertTrue(tpl.isSystem());
        assertEquals("name", tpl.getRequiredAttributes());
        assertEquals("meta_tpl_1", tpl.getMetaTemplateId());
        assertEquals("HIGH", tpl.getPriority());
        assertNotNull(tpl.getButtons());

        // AlertButtonEntity
        AlertButtonEntity btn = new AlertButtonEntity();
        btn.setId(id);
        btn.setTemplate(tpl);
        btn.setButtonText("Click");
        btn.setButtonType(ButtonType.QUICK_REPLY);
        btn.setPayload("payload_1");
        btn.setSequenceOrder(1);

        assertEquals(id, btn.getId());
        assertEquals(tpl, btn.getTemplate());
        assertEquals("Click", btn.getButtonText());
        assertEquals(ButtonType.QUICK_REPLY, btn.getButtonType());
        assertEquals("payload_1", btn.getPayload());
        assertEquals(1, btn.getSequenceOrder());

        // AlertButtonReplyEntity
        AlertButtonReplyEntity reply = new AlertButtonReplyEntity();
        reply.setId(id);
        reply.setButton(btn);
        reply.setReplyTemplate(tpl);
        reply.setRequiresHumanAgent(true);
        btn.setReply(reply);

        assertEquals(id, reply.getId());
        assertEquals(btn, reply.getButton());
        assertEquals(tpl, reply.getReplyTemplate());
        assertTrue(reply.isRequiresHumanAgent());
        assertEquals(reply, btn.getReply());

        // UserContactEntity
        UserContactEntity contact1 = new UserContactEntity(id, userId, AlertChannel.WHATSAPP, "+9876543210");
        assertEquals(id, contact1.getId());
        assertEquals(userId, contact1.getUserId());
        assertEquals(AlertChannel.WHATSAPP, contact1.getChannel());
        assertEquals("+9876543210", contact1.getContactValue());
        assertFalse(contact1.isOptedOut());
        assertTrue(contact1.isActive());
        assertNull(contact1.getHumanSessionExpiresAt());
        assertFalse(contact1.isHumanSessionActive());

        UserContactEntity contact2 = new UserContactEntity(id, userId, AlertChannel.WHATSAPP, "+9876543210", true, false, now.plusSeconds(300));
        assertTrue(contact2.isOptedOut());
        assertFalse(contact2.isActive());
        assertEquals(now.plusSeconds(300), contact2.getHumanSessionExpiresAt());
        assertTrue(contact2.isHumanSessionActive());

        contact2.setOptedOut(false);
        contact2.setActive(true);
        contact2.setContactValue("+111");
        contact2.setHumanSessionExpiresAt(now.minusSeconds(10));
        assertFalse(contact2.isOptedOut());
        assertTrue(contact2.isActive());
        assertEquals("+111", contact2.getContactValue());
        assertFalse(contact2.isHumanSessionActive());

        // AlertChannelConfigEntity
        AlertChannelConfigEntity cfg = new AlertChannelConfigEntity();
        cfg.setChannel(AlertChannel.WHATSAPP);
        cfg.setActive(false);
        cfg.setPauseReason("Rate limit exceeded");
        assertEquals(AlertChannel.WHATSAPP, cfg.getChannel());
        assertFalse(cfg.isActive());
        assertEquals("Rate limit exceeded", cfg.getPauseReason());

        // AlertTemplateCategoryEntity
        AlertTemplateCategoryEntity cat = new AlertTemplateCategoryEntity(id, "PROMO", "Promotion", "Promo templates", null);
        assertEquals("NORMAL", cat.getPriority());
        cat.setCategoryKey("NEW_KEY");
        cat.setName("New Name");
        cat.setDescription("New Desc");
        cat.setPriority("URGENT");
        assertEquals("NEW_KEY", cat.getCategoryKey());
        assertEquals("New Name", cat.getName());
        assertEquals("New Desc", cat.getDescription());
        assertEquals("URGENT", cat.getPriority());
    }
}
