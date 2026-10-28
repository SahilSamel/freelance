package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.template.seeder.TemplateSeeder;
import com.freelance.template.utils.TemplateRenderUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("System WhatsApp Templates Unit & Seeder Test Suite")
class SystemTemplatesTest {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_.]+)\\}\\}");

    static Stream<ITemplate> provideAllSystemTemplates() {
        return Stream.of(
                new ProspectWalkinRecapTemplate(),
                new CampaignSeasonalOfferTemplate(),
                new CampaignFestiveOfferTemplate(),
                new CampaignMilestoneOfferTemplate(),
                new MemberWelcomeOnboardingTemplate(),
                new MemberRenewalConfirmationTemplate(),
                new PaymentReceiptInstantTemplate(),
                new PlanRenewalReminderTemplate(),
                new UpcomingPaymentReminderTemplate(),
                new PaymentOverdueReminderTemplate(),
                new MemberLapsedWinbackTemplate(),
                new GoogleReviewRequestTemplate()
        );
    }

    @Test
    @DisplayName("Verify exactly 12 system templates are registered")
    void testSystemTemplateCount() {
        assertEquals(12, provideAllSystemTemplates().count(), "There should be exactly 12 gym templates defined");
    }

    @ParameterizedTest(name = "Validate metadata for template: {0}")
    @MethodSource("provideAllSystemTemplates")
    @DisplayName("Verify template metadata, naming conventions, and category")
    void testTemplateMetadata(ITemplate template) {
        assertNotNull(template.getTemplateKey(), "Template key must not be null");
        assertFalse(template.getTemplateKey().isBlank(), "Template key must not be blank");
        assertTrue(template.getTemplateKey().matches("^[a-z0-9_]+$"),
                "Template key must be lower-snake-case: " + template.getTemplateKey());

        assertNotNull(template.getName(), "Name must not be null");
        assertFalse(template.getName().isBlank(), "Name must not be blank");

        assertEquals(AlertChannel.WHATSAPP, template.getChannel(), "Channel must be WHATSAPP");
        assertTrue(template.getLanguage().startsWith("en"), "Language must be English (en / en_US)");

        // Category must be valid
        assertDoesNotThrow(() -> TemplateCategory.valueOf(template.getCategory().toUpperCase(Locale.ROOT)));

        assertNotNull(template.getContent(), "Content must not be null");
        assertFalse(template.getContent().isBlank(), "Content must not be blank");
    }

    @ParameterizedTest(name = "Validate placeholder consistency for: {0}")
    @MethodSource("provideAllSystemTemplates")
    @DisplayName("Verify that all {{DOMAIN.attribute}} placeholders match getRequiredAttributes()")
    void testPlaceholderConsistency(ITemplate template) {
        String content = template.getContent();
        Set<String> extractedPlaceholders = new HashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            extractedPlaceholders.add(matcher.group(1));
        }

        Set<String> declaredRequired = template.getRequiredAttributes() != null
                ? template.getRequiredAttributes()
                : Collections.emptySet();

        // Every placeholder used in content must be declared in getRequiredAttributes
        for (String placeholder : extractedPlaceholders) {
            assertTrue(declaredRequired.contains(placeholder),
                    String.format("Template '%s' uses '{{%s}}' in content but does not declare it in getRequiredAttributes()",
                            template.getTemplateKey(), placeholder));
        }

        // Every attribute declared as required must actually be in the content
        for (String declared : declaredRequired) {
            assertTrue(extractedPlaceholders.contains(declared),
                    String.format("Template '%s' declares required attribute '%s' but never uses it in content",
                            template.getTemplateKey(), declared));
        }
    }

    @ParameterizedTest(name = "Validate button structure for: {0}")
    @MethodSource("provideAllSystemTemplates")
    @DisplayName("Verify buttons comply with WhatsApp limits and payload guidelines")
    void testButtonsCompliance(ITemplate template) {
        List<ITemplateButton> buttons = template.getButtons();
        if (buttons == null || buttons.isEmpty()) {
            return;
        }

        assertTrue(buttons.size() <= 10,
                "Template '" + template.getTemplateKey() + "' exceeds WhatsApp limit of 10 buttons");

        Set<String> seenTexts = new HashSet<>();
        Set<Integer> seenOrders = new HashSet<>();

        for (ITemplateButton button : buttons) {
            assertNotNull(button.getText(), "Button text cannot be null");
            assertFalse(button.getText().isBlank(), "Button text cannot be blank");
            assertTrue(button.getText().length() <= 25,
                    "WhatsApp quick-reply / button text cannot exceed 25 chars: " + button.getText());

            assertTrue(seenTexts.add(button.getText().toLowerCase(Locale.ROOT)),
                    "Duplicate button text in template " + template.getTemplateKey() + ": " + button.getText());

            assertTrue(seenOrders.add(button.getSequenceOrder()),
                    "Duplicate button sequence order in template " + template.getTemplateKey() + ": " + button.getSequenceOrder());

            if (button.getType() == ButtonType.QUICK_REPLY) {
                if (button.requiresHumanAgent()) {
                    assertNotNull(button.getPayload(), "Human agent button must specify payload");
                } else if (button.getReplyTemplateKey() != null) {
                    assertFalse(button.getReplyTemplateKey().isBlank(), "Reply template key cannot be blank");
                } else {
                    assertNotNull(button.getPayload(), "Quick reply button must have either payload or replyTemplateKey");
                }
            } else if (button.getType() == ButtonType.URL) {
                assertNotNull(button.getPayload(), "URL button must contain URL in payload");
                assertTrue(button.getPayload().startsWith("http://") || button.getPayload().startsWith("https://") || button.getPayload().contains("{{"),
                        "URL button payload must start with http/https or be template token: " + button.getPayload());
            } else if (button.getType() == ButtonType.PHONE_NUMBER) {
                assertNotNull(button.getPayload(), "Phone button must contain phone number in payload");
            }
        }
    }

    @ParameterizedTest(name = "Render fidelity test for: {0}")
    @MethodSource("provideAllSystemTemplates")
    @DisplayName("Verify that template content renders cleanly without leftover placeholders")
    void testRenderFidelity(ITemplate template) {
        Map<String, String> mockValues = new HashMap<>();
        if (template.getRequiredAttributes() != null) {
            for (String attr : template.getRequiredAttributes()) {
                mockValues.put(attr, "MOCK_VAL_" + attr.replace('.', '_'));
            }
        }

        String rendered = TemplateRenderUtility.render(template.getContent(), mockValues);
        assertNotNull(rendered);
        assertFalse(rendered.contains("{{"),
                "Rendered content for " + template.getTemplateKey() + " still contains unresolved placeholder opening '{{': " + rendered);
        assertFalse(rendered.contains("}}"),
                "Rendered content for " + template.getTemplateKey() + " still contains unresolved placeholder closing '}}': " + rendered);
    }

    @Test
    @DisplayName("Verify PaymentReceiptInstantTemplate attributes and category")
    void testPaymentReceiptInstantMedia() {
        PaymentReceiptInstantTemplate receipt = new PaymentReceiptInstantTemplate();
        assertNull(receipt.getMediaType());
        assertEquals(TemplateCategory.UTILITY.name(), receipt.getCategory());
        assertTrue(receipt.getRequiredAttributes().contains("USER.first_name"));
    }

    @Test
    @DisplayName("Verify TemplateSeeder persists all 12 templates and configures buttons correctly")
    void testTemplateSeederPass2Chaining() {
        Map<String, AlertTemplateEntity> storage = new HashMap<>();
        AlertTemplateRepository fakeRepo = new AlertTemplateRepository() {
            @Override
            public Optional<AlertTemplateEntity> findByKey(String templateKey) {
                return Optional.ofNullable(storage.get(templateKey));
            }

            @Override
            public void persist(AlertTemplateEntity entity) {
                storage.put(entity.getTemplateKey(), entity);
            }

            @Override
            public AlertTemplateEntity merge(AlertTemplateEntity entity) {
                storage.put(entity.getTemplateKey(), entity);
                return entity;
            }
        };

        TemplateSeeder seeder = new TemplateSeeder(fakeRepo);
        List<ITemplate> allTemplates = provideAllSystemTemplates().toList();

        // Seed Pass 1 & Pass 2
        seeder.seed(allTemplates);

        assertEquals(12, storage.size(), "All 12 system templates must be stored");

        // Verify Member Welcome Onboarding has "Chat with Reception" button with human agent
        AlertTemplateEntity welcomeEntity = storage.get("member_welcome_onboarding");
        assertNotNull(welcomeEntity);
        assertNotNull(welcomeEntity.getButtons());
        assertEquals(1, welcomeEntity.getButtons().size());
        assertEquals("Chat with Reception", welcomeEntity.getButtons().get(0).getButtonText());
        assertNotNull(welcomeEntity.getButtons().get(0).getReply());
        assertTrue(welcomeEntity.getButtons().get(0).getReply().isRequiresHumanAgent());
    }
}
