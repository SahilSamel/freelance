package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class InboundWelcomeTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "inbound_welcome_menu";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Inbound Welcome Menu";
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
        return "Hello {{USER.first_name}}! Welcome to {{GYM.name}} assistant. 🤖\n\n"
                + "How can we help you today? Please choose an option below:";
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
        return Set.of("USER.first_name", "GYM.name");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.quickReplyWithFollowup("Membership & Dues", "MENU_MEMBER_SUMMARY", InboundMemberSummaryTemplate.TEMPLATE_KEY, 1),
                TemplateButtonRecord.quickReplyWithFollowup("Gym Hours & Info", "MENU_GYM_INFO", InboundGymInfoTemplate.TEMPLATE_KEY, 2),
                TemplateButtonRecord.quickReplyWithFollowup("Plans & Pricing", "MENU_SERVICES", InboundServicesInfoTemplate.TEMPLATE_KEY, 3),
                TemplateButtonRecord.humanSupport("Chat with Reception", 4)
        );
    }
}
