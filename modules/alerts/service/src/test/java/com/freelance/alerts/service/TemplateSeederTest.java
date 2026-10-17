package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.template.seeder.TemplateSeeder;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.manager.CacheManager;
import com.freelance.system.SystemHardcoding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TemplateSeederTest {

    static class FakeAlertTemplateRepository extends AlertTemplateRepository {
        final Map<String, AlertTemplateEntity> storage = new HashMap<>();
        int persistCalls = 0;
        int mergeCalls = 0;

        @Override
        public Optional<AlertTemplateEntity> findByKey(String templateKey) {
            return Optional.ofNullable(storage.get(templateKey));
        }

        @Override
        public void persist(AlertTemplateEntity entity) {
            persistCalls++;
            storage.put(entity.getTemplateKey(), entity);
        }

        @Override
        public AlertTemplateEntity merge(AlertTemplateEntity entity) {
            mergeCalls++;
            storage.put(entity.getTemplateKey(), entity);
            return entity;
        }
    }

    @Test
    @DisplayName("Should insert new template when not existing in repository")
    void testInsertNewTemplate() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();
        ITemplate tpl = mockTemplate("TPL_NEW", "Welcome", "Welcome to {{user.username}}", null);

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(tpl));

        assertEquals(1, fakeRepo.persistCalls);
        AlertTemplateEntity persisted = fakeRepo.storage.get("TPL_NEW");
        assertNotNull(persisted);
        assertEquals("TPL_NEW", persisted.getTemplateKey());
        assertEquals("Welcome", persisted.getName());
        assertEquals("Welcome to {{user.username}}", persisted.getContent());
        assertEquals(TemplateStatus.DRAFT, persisted.getStatus());
        assertTrue(persisted.isSystem());
    }

    @Test
    @DisplayName("Should update existing system template when code template has changed and not edited by user")
    void testUpdateExistingSystemTemplate() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();

        AlertTemplateEntity existing = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_WELCOME",
                "Old Welcome",
                "ONBOARDING",
                "en",
                "Old content",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                "user.username",
                null
        );
        // lastUpdatedBy is system UUID
        existing.setLastUpdatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
        fakeRepo.storage.put("TPL_WELCOME", existing);

        ITemplate updatedTpl = mockTemplate("TPL_WELCOME", "New Welcome", "New content with {{user.username}}", Set.of("user.username", "user.phone"));

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(updatedTpl));

        assertEquals(1, fakeRepo.mergeCalls);
        assertEquals("New Welcome", existing.getName());
        assertEquals("New content with {{user.username}}", existing.getContent());
        assertTrue(existing.getRequiredAttributes().contains("user.phone"));
    }

    @Test
    @DisplayName("Should NOT overwrite existing template if modified by custom user")
    void testSkipUpdateWhenModifiedByCustomUser() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();

        AlertTemplateEntity existing = new AlertTemplateEntity(
                UUID.randomUUID(),
                "TPL_CUSTOMIZED",
                "Admin Custom Name",
                "ACCOUNT",
                "en",
                "Custom admin content",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                "user.username",
                null
        );
        // Custom user modified this template
        existing.setLastUpdatedBy(UUID.randomUUID());
        fakeRepo.storage.put("TPL_CUSTOMIZED", existing);

        ITemplate codeTpl = mockTemplate("TPL_CUSTOMIZED", "Code Name", "Code content", null);

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(codeTpl));

        assertEquals(0, fakeRepo.mergeCalls);
        assertEquals("Admin Custom Name", existing.getName());
        assertEquals("Custom admin content", existing.getContent());
    }

    @Test
    @DisplayName("Should use configured default channel when template channel is null")
    void testDefaultChannelWhenNull() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();
        ITemplate tpl = new ITemplate() {
            @Override public String getTemplateKey() { return "TPL_DEFAULT_CH"; }
            @Override public String getName() { return "Default Ch Template"; }
            @Override public String getCategory() { return "UTILITY"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Hello"; }
            @Override public String getMediaUrl() { return null; }
            @Override public String getMediaType() { return null; }
            @Override public AlertChannel getChannel() { return null; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public List<ITemplateButton> getButtons() { return Collections.emptyList(); }
        };

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(tpl));

        AlertTemplateEntity persisted = fakeRepo.storage.get("TPL_DEFAULT_CH");
        assertNotNull(persisted);
        assertEquals(AlertChannel.WHATSAPP, persisted.getChannel());
    }

    private ITemplate mockTemplate(String key, String name, String content, Set<String> attrs) {
        return new ITemplate() {
            @Override
            public String getTemplateKey() { return key; }
            @Override
            public String getName() { return name; }
            @Override
            public String getCategory() { return "ACCOUNT"; }
            @Override
            public String getLanguage() { return "en"; }
            @Override
            public String getContent() { return content; }
            @Override
            public String getMediaUrl() { return null; }
            @Override
            public String getMediaType() { return null; }
            @Override
            public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override
            public Set<String> getRequiredAttributes() { return attrs != null ? attrs : Collections.emptySet(); }
            @Override
            public List<ITemplateButton> getButtons() { return Collections.emptyList(); }
            @Override
            public TemplateStatus getInitialStatus() { return TemplateStatus.DRAFT; }
        };
    }

    @Test
    @DisplayName("Should populate pre-approved metaTemplateId when provided by ITemplate")
    void testInsertNewTemplateWithPreapprovedMetaTemplateId() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();
        ITemplate tpl = new ITemplate() {
            @Override public String getTemplateKey() { return "TPL_PREAPPROVED"; }
            @Override public String getName() { return "Preapproved"; }
            @Override public String getCategory() { return "UTILITY"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Hello"; }
            @Override public String getMediaUrl() { return null; }
            @Override public String getMediaType() { return null; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public List<ITemplateButton> getButtons() { return Collections.emptyList(); }
            @Override public String getMetaTemplateId() { return "meta_preapproved_999"; }
        };

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(tpl));

        AlertTemplateEntity persisted = fakeRepo.storage.get("TPL_PREAPPROVED");
        assertNotNull(persisted);
        assertEquals("meta_preapproved_999", persisted.getMetaTemplateId());
        assertEquals(TemplateStatus.APPROVED, persisted.getStatus());
    }

    @Test
    @DisplayName("Should update existing template metaTemplateId when ITemplate provides new pre-approved ID")
    void testUpdateExistingSystemTemplateMetaTemplateId() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();
        AlertTemplateEntity existing = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_UPGRADE", "Upgrade", "UTILITY", "en", "Content",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
        );
        existing.setLastUpdatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
        fakeRepo.storage.put("TPL_UPGRADE", existing);

        ITemplate updatedTpl = new ITemplate() {
            @Override public String getTemplateKey() { return "TPL_UPGRADE"; }
            @Override public String getName() { return "Upgrade"; }
            @Override public String getCategory() { return "UTILITY"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Content"; }
            @Override public String getMediaUrl() { return null; }
            @Override public String getMediaType() { return null; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public List<ITemplateButton> getButtons() { return Collections.emptyList(); }
            @Override public String getMetaTemplateId() { return "meta_newly_approved_111"; }
        };

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(updatedTpl));

        assertEquals(1, fakeRepo.mergeCalls);
        assertEquals("meta_newly_approved_111", existing.getMetaTemplateId());
    }

    @Test
    @DisplayName("Should handle null or empty seed input")
    void testSeedNullOrEmpty() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();
        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        assertDoesNotThrow(() -> seeder.seed(null));
        assertDoesNotThrow(() -> seeder.seed(Collections.emptyList()));
        assertEquals(0, fakeRepo.persistCalls);
        assertEquals(0, fakeRepo.mergeCalls);
    }

    @Test
    @DisplayName("Should seed template with buttons and reply entities")
    void testSeedWithButtons() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();

        ITemplateButton btn1 = new ITemplateButton() {
            @Override public String getText() { return "Approve"; }
            @Override public com.freelance.alerts.ButtonType getType() { return com.freelance.alerts.ButtonType.QUICK_REPLY; }
            @Override public String getPayload() { return "approve_payload"; }
            @Override public String getReplyTemplateKey() { return null; }
            @Override public int getSequenceOrder() { return 1; }
            @Override public boolean requiresHumanAgent() { return false; }
        };

        ITemplateButton btn2 = new ITemplateButton() {
            @Override public String getText() { return "Talk to Agent"; }
            @Override public com.freelance.alerts.ButtonType getType() { return com.freelance.alerts.ButtonType.QUICK_REPLY; }
            @Override public String getPayload() { return "agent_payload"; }
            @Override public String getReplyTemplateKey() { return null; }
            @Override public int getSequenceOrder() { return 2; }
            @Override public boolean requiresHumanAgent() { return true; }
        };

        ITemplate tpl = new ITemplate() {
            @Override public String getTemplateKey() { return "TPL_WITH_BTNS"; }
            @Override public String getName() { return "Buttons Template"; }
            @Override public String getCategory() { return "SERVICE"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Please choose:"; }
            @Override public String getMediaUrl() { return "https://cdn.example.com/img.png"; }
            @Override public String getMediaType() { return "image/png"; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public List<ITemplateButton> getButtons() { return List.of(btn1, btn2); }
            @Override public String getPriority() { return "HIGH"; }
        };

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(tpl));

        AlertTemplateEntity persisted = fakeRepo.storage.get("TPL_WITH_BTNS");
        assertNotNull(persisted);
        assertEquals(2, persisted.getButtons().size());
        assertEquals("Approve", persisted.getButtons().get(0).getButtonText());
        assertFalse(persisted.getButtons().get(0).getReply().isRequiresHumanAgent());
        assertEquals("Talk to Agent", persisted.getButtons().get(1).getButtonText());
        assertTrue(persisted.getButtons().get(1).getReply().isRequiresHumanAgent());
    }

    @Test
    @DisplayName("Should synchronize all modified fields including media, priority, and buttons")
    void testSynchronizeAllModifiedFields() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();

        AlertTemplateEntity existing = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_ALL_FIELDS", "Old Name", "CAT_1", "en", "Old Body",
                "https://old.url", "image/jpeg", AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, "old.attr", "old_meta", "LOW"
        );
        existing.setLastUpdatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
        fakeRepo.storage.put("TPL_ALL_FIELDS", existing);

        ITemplateButton newBtn = new ITemplateButton() {
            @Override public String getText() { return "New Button"; }
            @Override public com.freelance.alerts.ButtonType getType() { return com.freelance.alerts.ButtonType.QUICK_REPLY; }
            @Override public String getPayload() { return "new_p"; }
            @Override public String getReplyTemplateKey() { return null; }
            @Override public int getSequenceOrder() { return 1; }
            @Override public boolean requiresHumanAgent() { return false; }
        };

        ITemplate updatedTpl = new ITemplate() {
            @Override public String getTemplateKey() { return "TPL_ALL_FIELDS"; }
            @Override public String getName() { return "New Name"; }
            @Override public String getCategory() { return "CAT_2"; }
            @Override public String getLanguage() { return "es"; }
            @Override public String getContent() { return "New Body"; }
            @Override public String getMediaUrl() { return "https://new.url"; }
            @Override public String getMediaType() { return "image/png"; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Set.of("new.attr"); }
            @Override public List<ITemplateButton> getButtons() { return List.of(newBtn); }
            @Override public String getMetaTemplateId() { return "new_meta_id"; }
            @Override public String getPriority() { return "HIGH"; }
        };

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(updatedTpl));

        assertEquals(1, fakeRepo.mergeCalls);
        assertEquals("New Name", existing.getName());
        assertEquals("CAT_2", existing.getCategory());
        assertEquals("es", existing.getLanguage());
        assertEquals("New Body", existing.getContent());
        assertEquals("https://new.url", existing.getMediaUrl());
        assertEquals("image/png", existing.getMediaType());
        assertEquals("new.attr", existing.getRequiredAttributes());
        assertEquals("new_meta_id", existing.getMetaTemplateId());
        assertEquals("HIGH", existing.getPriority());
        assertEquals(1, existing.getButtons().size());
    }

    @Test
    @DisplayName("Should test default constructor")
    void testDefaultConstructor() {
        TemplateSeeder seeder = new TemplateSeeder();
        assertNotNull(seeder);
    }

    @Test
    @DisplayName("Should detect button changes when attributes differ")
    void testButtonChangeVariations() {
        FakeAlertTemplateRepository fakeRepo = new FakeAlertTemplateRepository();

        // 1. Buttons cleared when code template buttons is empty
        AlertTemplateEntity existing1 = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_BTN_1", "Name", "CAT", "en", "Body",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
        );
        existing1.setLastUpdatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
        existing1.getButtons().add(new com.freelance.alerts.template.entity.AlertButtonEntity(
                UUID.randomUUID(), existing1, "Old Btn", com.freelance.alerts.ButtonType.QUICK_REPLY, "p1", 1
        ));
        fakeRepo.storage.put("TPL_BTN_1", existing1);

        ITemplate codeTpl1 = mockTemplate("TPL_BTN_1", "Name", "Body", null);
        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        seeder.seed(List.of(codeTpl1));

        assertEquals(1, fakeRepo.mergeCalls);
        assertTrue(existing1.getButtons().isEmpty());

        // 2. Buttons changed when sizes differ
        AlertTemplateEntity existing2 = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_BTN_2", "Name", "CAT", "en", "Body",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
        );
        existing2.setLastUpdatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
        fakeRepo.storage.put("TPL_BTN_2", existing2);

        ITemplate codeTpl2 = new ITemplate() {
            @Override public String getTemplateKey() { return "TPL_BTN_2"; }
            @Override public String getName() { return "Name"; }
            @Override public String getCategory() { return "CAT"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Body"; }
            @Override public String getMediaUrl() { return null; }
            @Override public String getMediaType() { return null; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public List<ITemplateButton> getButtons() {
                return List.of(new ITemplateButton() {
                    @Override public String getText() { return "Btn"; }
                    @Override public com.freelance.alerts.ButtonType getType() { return com.freelance.alerts.ButtonType.QUICK_REPLY; }
                    @Override public String getPayload() { return "p"; }
                    @Override public String getReplyTemplateKey() { return null; }
                    @Override public int getSequenceOrder() { return 1; }
                    @Override public boolean requiresHumanAgent() { return false; }
                });
            }
        };
        seeder.seed(List.of(codeTpl2));
        assertEquals(2, fakeRepo.mergeCalls);
        assertEquals(1, existing2.getButtons().size());

        // 3. Button property mismatches: text, type, payload, sequence, requiresHuman
        // Text mismatch
        AlertTemplateEntity existing3 = new AlertTemplateEntity(
                UUID.randomUUID(), "TPL_BTN_3", "Name", "CAT", "en", "Body",
                null, null, AlertChannel.WHATSAPP, TemplateStatus.APPROVED, true, null, null
        );
        existing3.setLastUpdatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
        existing3.getButtons().add(new com.freelance.alerts.template.entity.AlertButtonEntity(
                UUID.randomUUID(), existing3, "Different Text", com.freelance.alerts.ButtonType.QUICK_REPLY, "p", 1
        ));
        fakeRepo.storage.put("TPL_BTN_3", existing3);

        ITemplate codeTpl3 = new ITemplate() {
            @Override public String getTemplateKey() { return "TPL_BTN_3"; }
            @Override public String getName() { return "Name"; }
            @Override public String getCategory() { return "CAT"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Body"; }
            @Override public String getMediaUrl() { return null; }
            @Override public String getMediaType() { return null; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public List<ITemplateButton> getButtons() {
                return List.of(new ITemplateButton() {
                    @Override public String getText() { return "Expected Text"; }
                    @Override public com.freelance.alerts.ButtonType getType() { return com.freelance.alerts.ButtonType.QUICK_REPLY; }
                    @Override public String getPayload() { return "p"; }
                    @Override public String getReplyTemplateKey() { return null; }
                    @Override public int getSequenceOrder() { return 1; }
                    @Override public boolean requiresHumanAgent() { return false; }
                });
            }
        };
        seeder.seed(List.of(codeTpl3));
        assertEquals(3, fakeRepo.mergeCalls);

        // Type mismatch
        existing3.getButtons().get(0).setButtonText("Expected Text");
        existing3.getButtons().get(0).setButtonType(com.freelance.alerts.ButtonType.URL);
        seeder.seed(List.of(codeTpl3));
        assertEquals(4, fakeRepo.mergeCalls);

        // Payload mismatch
        existing3.getButtons().get(0).setButtonType(com.freelance.alerts.ButtonType.QUICK_REPLY);
        existing3.getButtons().get(0).setPayload("different_payload");
        seeder.seed(List.of(codeTpl3));
        assertEquals(5, fakeRepo.mergeCalls);

        // Sequence mismatch
        existing3.getButtons().get(0).setPayload("p");
        existing3.getButtons().get(0).setSequenceOrder(99);
        seeder.seed(List.of(codeTpl3));
        assertEquals(6, fakeRepo.mergeCalls);

        // Human agent mismatch
        existing3.getButtons().get(0).setSequenceOrder(1);
        existing3.getButtons().get(0).setReply(new com.freelance.alerts.template.entity.AlertButtonReplyEntity(
                UUID.randomUUID(), existing3.getButtons().get(0), null, true
        ));
        seeder.seed(List.of(codeTpl3));
        assertEquals(7, fakeRepo.mergeCalls);

        // 4. Exact match -> No merge called!
        existing3.getButtons().get(0).setReply(new com.freelance.alerts.template.entity.AlertButtonReplyEntity(
                UUID.randomUUID(), existing3.getButtons().get(0), null, false
        ));
        int mergeCallsBefore = fakeRepo.mergeCalls;
        seeder.seed(List.of(codeTpl3));
        assertEquals(mergeCallsBefore, fakeRepo.mergeCalls);
    }
}