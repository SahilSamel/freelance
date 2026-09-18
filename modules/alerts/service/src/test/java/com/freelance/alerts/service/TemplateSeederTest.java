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
}