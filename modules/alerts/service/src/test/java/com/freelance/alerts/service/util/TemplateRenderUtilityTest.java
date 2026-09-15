package com.freelance.alerts.service.util;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.alerts.template.ITemplate;
import com.freelance.template.utils.TemplateRenderUtility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateRenderUtilityTest {

    @Test
    @DisplayName("Should render String template with matching placeholders")
    void testRenderStringTemplate() {
        String template = "Hello {{user.name}}, your code is {{code}}!";
        Map<String, Object> data = Map.of(
                "user.name", "Sahil",
                "code", 123456
        );

        String result = TemplateRenderUtility.render(template, data);
        assertEquals("Hello Sahil, your code is 123456!", result);
    }

    @Test
    @DisplayName("Should render AlertTemplateEntity content with data")
    void testRenderAlertTemplateEntity() {
        AlertTemplateEntity entity = new AlertTemplateEntity(
                UUID.randomUUID(),
                "OTP_TPL",
                "OTP Verification",
                "AUTHENTICATION",
                "en",
                "Your OTP is {{otp}}. Valid for {{mins}} mins.",
                null,
                null,
                AlertChannel.WHATSAPP,
                TemplateStatus.APPROVED,
                true,
                "otp,mins",
                null
        );

        Map<String, String> data = Map.of("otp", "9876", "mins", "10");
        String result = TemplateRenderUtility.render(entity, data);
        assertEquals("Your OTP is 9876. Valid for 10 mins.", result);
    }

    @Test
    @DisplayName("Should render ITemplate content with data")
    void testRenderITemplate() {
        ITemplate template = new ITemplate() {
            @Override public String getTemplateKey() { return "TEST_TPL"; }
            @Override public String getName() { return "Test"; }
            @Override public String getCategory() { return "UTILITY"; }
            @Override public String getLanguage() { return "en"; }
            @Override public String getContent() { return "Order {{order.id}} confirmed for {{amount}}."; }
            @Override public String getMediaUrl() { return null; }
            @Override public String getMediaType() { return null; }
            @Override public AlertChannel getChannel() { return AlertChannel.WHATSAPP; }
            @Override public Set<String> getRequiredAttributes() { return Collections.emptySet(); }
            @Override public java.util.List<com.freelance.alerts.template.ITemplateButton> getButtons() { return Collections.emptyList(); }
        };

        Map<String, String> data = Map.of("order.id", "ORD-101", "amount", "$50");
        String result = TemplateRenderUtility.render(template, data);
        assertEquals("Order ORD-101 confirmed for $50.", result);
    }

    @Test
    @DisplayName("Should handle null and empty inputs gracefully")
    void testRenderNullAndEmptyInputs() {
        assertEquals("", TemplateRenderUtility.render((AlertTemplateEntity) null, Map.of()));
        assertEquals("", TemplateRenderUtility.render((ITemplate) null, Map.of()));
        assertEquals("", TemplateRenderUtility.render((String) null, Map.of()));
        assertEquals("Plain text", TemplateRenderUtility.render("Plain text", null));
        assertEquals("Plain text", TemplateRenderUtility.render("Plain text", Collections.emptyMap()));
    }

    @Test
    @DisplayName("Should replace missing data with empty string when null value mapped")
    void testRenderWithNullValueInMap() {
        String template = "Welcome {{name}}!";
        Map<String, String> data = Collections.singletonMap("name", null);
        String result = TemplateRenderUtility.render(template, data);
        assertEquals("Welcome !", result);
    }
}
