package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class InboundServicesInfoTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "inbound_services_info";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Inbound Plans & Pricing Catalogue";
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
        return "Available Membership Plans at {{GYM.name}}: 📋\n\n"
                + "{{GYM.plan_catalogue}}\n\n"
                + "Visit the front desk or chat with reception below to enroll or upgrade your plan!";
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
        return Set.of("GYM.name", "GYM.plan_catalogue");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.quickReplyWithFollowup("Main Menu", "INBOUND_WELCOME_MENU", InboundWelcomeTemplate.TEMPLATE_KEY, 1),
                TemplateButtonRecord.humanSupport("Chat with Reception", 2)
        );
    }
}
