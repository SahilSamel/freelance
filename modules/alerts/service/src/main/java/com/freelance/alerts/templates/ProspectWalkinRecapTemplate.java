package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.DispatchScope;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class ProspectWalkinRecapTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "prospect_walkin_recap";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Walk-In Enquiry & Quote Recap";
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
        return "Hey {{USER.first_name}}, great speaking with you at *{{GYM.name}}*! 👋\n\n"
                + "Here is a quick recap of the details we discussed:\n"
                + "{{CUSTOM.recap_details}}\n\n"
                + "📍 *Location:* {{GYM.google_maps_url}}\n"
                + "⏰ *Timings:* {{GYM.timings}}\n"
                + "📞 *Front Desk:* {{GYM.support_phone}}\n\n"
                + "Have a question about the plans or ready to get started? Tap below to chat with our front desk!";
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
        return Set.of(
                "USER.first_name",
                "GYM.name",
                "CUSTOM.recap_details",
                "GYM.timings",
                "GYM.support_phone",
                "GYM.google_maps_url"
        );
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.humanSupport("Chat with Reception", 1)
        );
    }

    @Override
    public com.freelance.alerts.template.DispatchScope getDispatchScope() {
        return DispatchScope.INDIVIDUAL;
    }
}
