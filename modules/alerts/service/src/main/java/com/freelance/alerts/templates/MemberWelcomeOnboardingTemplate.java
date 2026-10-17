package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class MemberWelcomeOnboardingTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "member_welcome_onboarding";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Member Welcome & Onboarding";
    }

    @Override
    public String getCategory() {
        return TemplateCategory.UTILITY.name();
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }

    @Override
    public String getContent() {
        return "Welcome to *{{GYM.name}}*, {{USER.first_name}}! 🏋️🔥\n\n"
                + "Your membership is now active. Here is your plan summary:\n"
                + "{{MEMBER.plans_breakdown}}\n\n"
                + "🎒 *Quick Floor Essentials:*\n"
                + "• Carry a clean pair of indoor training shoes and a workout towel.\n"
                + "• Please re-rack your weights after your sets to keep the floor safe.\n\n"
                + "⏰ *Timings:* {{GYM.timings}}\n"
                + "📞 *Front Desk:* {{GYM.support_phone}}\n\n"
                + "Tap *Open Menu* or message us here anytime to check your plan validity, payments, or schedule. Let's have a great first session!";
    }

    @Override
    public String getMediaUrl() {
        return null;
    }

    @Override
    public String getMediaType() {
        return null;
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.WHATSAPP;
    }

    @Override
    public Set<String> getRequiredAttributes() {
        return Set.of("GYM.name", "USER.first_name", "MEMBER.plans_breakdown", "GYM.timings", "GYM.support_phone");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.quickReplyWithFollowup("Open Menu", "INBOUND_WELCOME_MENU", InboundWelcomeTemplate.TEMPLATE_KEY, 1),
                TemplateButtonRecord.humanSupport("Chat with Reception", 2)
        );
    }

    @Override
    public com.freelance.alerts.template.DispatchScope getDispatchScope() {
        return com.freelance.alerts.template.DispatchScope.AUTO;
    }
}
