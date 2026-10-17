package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class MemberRenewalConfirmationTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "member_renewal_confirmation";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Member Renewal Confirmation";
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
        return "Hey {{USER.first_name}}, your membership at *{{GYM.name}}* is officially renewed! 💪🔥\n\n"
                + "Here is a quick summary of your active plan:\n"
                + "{{MEMBER.plans_breakdown}}\n\n"
                + "⏰ *Timings:* {{GYM.timings}}\n"
                + "📞 *Front Desk:* {{GYM.support_phone}}\n\n"
                + "Tap *Open Menu* below anytime to view your active plans, payment history, or schedule. See you on the floor!";
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
