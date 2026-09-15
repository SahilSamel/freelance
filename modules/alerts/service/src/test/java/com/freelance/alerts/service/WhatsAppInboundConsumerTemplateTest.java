package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.dto.webhook.*;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.consumer.alerts.inbound.whatsapp.WhatsAppInboundConsumer;
import com.freelance.event.model.EventMessage;
import com.freelance.fixtures.TestContextHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppInboundConsumerTemplateTest {

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<String, AlertTemplateEntity> byKey = new HashMap<>();
        final Map<String, AlertTemplateEntity> byMetaId = new HashMap<>();

        @Override
        public Optional<AlertTemplateEntity> findByKey(String templateKey) {
            if (templateKey == null || templateKey.isBlank()) return Optional.empty();
            return Optional.ofNullable(byKey.get(templateKey));
        }

        @Override
        public Optional<AlertTemplateEntity> findByKeyIgnoreCase(String templateKey) {
            if (templateKey == null || templateKey.isBlank()) return Optional.empty();
            return byKey.values().stream()
                    .filter(t -> t.getTemplateKey().equalsIgnoreCase(templateKey.trim()))
                    .findFirst();
        }

        @Override
        public Optional<AlertTemplateEntity> findByMetaTemplateId(String metaTemplateId) {
            if (metaTemplateId == null || metaTemplateId.isBlank()) return Optional.empty();
            return Optional.ofNullable(byMetaId.get(metaTemplateId));
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
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

    private TransactionContext context;
    private FakeAlertTemplateRepository templateRepo;
    private WhatsAppInboundConsumer consumer;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        templateRepo = new FakeAlertTemplateRepository();
        consumer = new WhatsAppInboundConsumer(
                null,
                null,
                null,
                null,
                templateRepo
        );
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should update template status to APPROVED and store metaTemplateId on APPROVED webhook event")
    void testTemplateStatusApproved() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "tpl_promo", "Promo", "MARKETING", "en", "Promo content",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null
        );
        templateRepo.persist(entity);

        WebhookTemplateStatusDTO statusDTO = new WebhookTemplateStatusDTO(
                "APPROVED", "meta_12345", "tpl_promo", "en", null
        );
        WebhookValueDTO valueDTO = new WebhookValueDTO("whatsapp_business_account", null, null, null, statusDTO);
        WebhookChangeDTO changeDTO = new WebhookChangeDTO("message_template_status_update", valueDTO);
        WebhookEntryDTO entryDTO = new WebhookEntryDTO("entry_1", List.of(changeDTO));
        WhatsAppWebhookPayloadDTO payload = new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entryDTO));

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", payload));

        AlertTemplateEntity updated = templateRepo.byKey.get("tpl_promo");
        assertNotNull(updated);
        assertEquals(TemplateStatus.APPROVED, updated.getStatus());
        assertEquals("meta_12345", updated.getMetaTemplateId());
    }

    @Test
    @DisplayName("Should update template status to REJECTED on REJECTED webhook event")
    void testTemplateStatusRejected() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "tpl_promo_rej", "Promo", "MARKETING", "en", "Promo content",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null
        );
        templateRepo.persist(entity);

        WebhookTemplateStatusDTO statusDTO = new WebhookTemplateStatusDTO(
                "REJECTED", "meta_67890", "tpl_promo_rej", "en", "Content policy violation"
        );
        WebhookValueDTO valueDTO = new WebhookValueDTO("whatsapp_business_account", null, null, null, statusDTO);
        WebhookChangeDTO changeDTO = new WebhookChangeDTO("message_template_status_update", valueDTO);
        WebhookEntryDTO entryDTO = new WebhookEntryDTO("entry_1", List.of(changeDTO));
        WhatsAppWebhookPayloadDTO payload = new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entryDTO));

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", payload));

        AlertTemplateEntity updated = templateRepo.byKey.get("tpl_promo_rej");
        assertNotNull(updated);
        assertEquals(TemplateStatus.REJECTED, updated.getStatus());
        assertEquals("meta_67890", updated.getMetaTemplateId());
    }

    @Test
    @DisplayName("Should update template status to PAUSED on PAUSED webhook event")
    void testTemplateStatusPaused() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "tpl_promo_pause", "Promo", "MARKETING", "en", "Promo content",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, false, null, "meta_pause_1"
        );
        templateRepo.persist(entity);

        WebhookTemplateStatusDTO statusDTO = new WebhookTemplateStatusDTO(
                "PAUSED", "meta_pause_1", "tpl_promo_pause", "en", "Quality low"
        );
        WebhookValueDTO valueDTO = new WebhookValueDTO("whatsapp_business_account", null, null, null, statusDTO);
        WebhookChangeDTO changeDTO = new WebhookChangeDTO("message_template_status_update", valueDTO);
        WebhookEntryDTO entryDTO = new WebhookEntryDTO("entry_1", List.of(changeDTO));
        WhatsAppWebhookPayloadDTO payload = new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entryDTO));

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", payload));

        AlertTemplateEntity updated = templateRepo.byKey.get("tpl_promo_pause");
        assertNotNull(updated);
        assertEquals(TemplateStatus.PAUSED, updated.getStatus());
    }

    @Test
    @DisplayName("Should match template case-insensitively when Meta returns lowercase name")
    void testCaseInsensitiveTemplateMatch() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_UPPERCASE_KEY", "Uppercase", "UTILITY", "en", "Text",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null
        );
        templateRepo.persist(entity);

        WebhookTemplateStatusDTO statusDTO = new WebhookTemplateStatusDTO(
                "APPROVED", "meta_case_1", "tpl_uppercase_key", "en", null
        );
        WebhookValueDTO valueDTO = new WebhookValueDTO("whatsapp_business_account", null, null, null, statusDTO);
        WebhookChangeDTO changeDTO = new WebhookChangeDTO("message_template_status_update", valueDTO);
        WebhookEntryDTO entryDTO = new WebhookEntryDTO("entry_1", List.of(changeDTO));
        WhatsAppWebhookPayloadDTO payload = new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entryDTO));

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", payload));

        AlertTemplateEntity updated = templateRepo.byKey.get("TPL_UPPERCASE_KEY");
        assertNotNull(updated);
        assertEquals(TemplateStatus.APPROVED, updated.getStatus());
        assertEquals("meta_case_1", updated.getMetaTemplateId());
    }

    @Test
    @DisplayName("Should match template by metaTemplateId when template name does not match directly")
    void testMetaTemplateIdFallbackMatch() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_CUSTOM_KEY", "Custom", "UTILITY", "en", "Text",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, "meta_id_exact_match"
        );
        templateRepo.persist(entity);

        WebhookTemplateStatusDTO statusDTO = new WebhookTemplateStatusDTO(
                "APPROVED", "meta_id_exact_match", "unknown_meta_name", "en", null
        );
        WebhookValueDTO valueDTO = new WebhookValueDTO("whatsapp_business_account", null, null, null, statusDTO);
        WebhookChangeDTO changeDTO = new WebhookChangeDTO("message_template_status_update", valueDTO);
        WebhookEntryDTO entryDTO = new WebhookEntryDTO("entry_1", List.of(changeDTO));
        WhatsAppWebhookPayloadDTO payload = new WhatsAppWebhookPayloadDTO("whatsapp_business_account", List.of(entryDTO));

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", payload));

        AlertTemplateEntity updated = templateRepo.byKey.get("TPL_CUSTOM_KEY");
        assertNotNull(updated);
        assertEquals(TemplateStatus.APPROVED, updated.getStatus());
    }

    @Test
    @DisplayName("Should handle official Meta flat payload with numeric template id via JSON deserialization")
    void testOfficialMetaFlatPayloadJson() throws Exception {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "custom_invoice_notification", "Invoice", "UTILITY", "en_US", "Text",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null
        );
        templateRepo.persist(entity);

        String json = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
                      "changes": [
                        {
                          "field": "message_template_status_update",
                          "value": {
                            "event": "APPROVED",
                            "message_template_id": 123456789012345,
                            "message_template_name": "custom_invoice_notification",
                            "message_template_language": "en_US",
                            "reason": null
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        WhatsAppWebhookPayloadDTO payload = mapper.readValue(json, WhatsAppWebhookPayloadDTO.class);

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", payload));

        AlertTemplateEntity updated = templateRepo.byKey.get("custom_invoice_notification");
        assertNotNull(updated);
        assertEquals(TemplateStatus.APPROVED, updated.getStatus());
        assertEquals("123456789012345", updated.getMetaTemplateId());
    }

    @Test
    @DisplayName("Should handle user payload where field is template key and value is flat")
    void testUserProvidedPayloadWhereFieldIsTemplateName() throws Exception {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(), "custom_invoice_notification", "Invoice", "UTILITY", "en_US", "Text",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.SUBMITTED, false, null, null
        );
        templateRepo.persist(entity);

        String json = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "WHATSAPP_BUSINESS_ACCOUNT_ID",
                      "changes": [
                        {
                          "field": "custom_invoice_notification",
                          "value": {
                            "event": "APPROVED",
                            "message_template_id": "123456789012345",
                            "message_template_name": "custom_invoice_notification",
                            "message_template_language": "en_US"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        WhatsAppWebhookPayloadDTO payload = mapper.readValue(json, WhatsAppWebhookPayloadDTO.class);

        consumer.handle(EventMessage.of("alert.inbound.whatsapp", payload));

        AlertTemplateEntity updated = templateRepo.byKey.get("custom_invoice_notification");
        assertNotNull(updated);
        assertEquals(TemplateStatus.APPROVED, updated.getStatus());
        assertEquals("123456789012345", updated.getMetaTemplateId());
    }
}
