package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.DispatchScope;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class CampaignMilestoneOfferTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "campaign_milestone_offer";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Special Announcement & Flash Offer";
    }

    @Override
    public String getCategory() {
        return TemplateCategory.MARKETING.name();
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }

    @Override
    public String getContent() {
        return "Hey {{USER.first_name}}, we have something special lined up for you at {{GYM.name}}! 🔥\n\n"
                + "*{{CUSTOM.headline}}*\n"
                + "{{CUSTOM.offer_details}}\n\n"
                + "Whether you're looking to kickstart your routine or level up your training, this is the best time to lock in your spot.\n\n"
                + "📍 Location: {{GYM.google_maps_url}}\n"
                + "⏰ Timings: {{GYM.timings}}\n"
                + "📞 Front Desk: {{GYM.support_phone}}\n\n"
                + "Tap *Claim Offer* below to grab this deal before slots close!\n\n"
                + "Reply STOP to unsubscribe from promotional updates.";
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
    public DispatchScope getDispatchScope() {
        return DispatchScope.BOTH;
    }

    @Override
    public Set<String> getRequiredAttributes() {
        return Set.of(
                "USER.first_name",
                "GYM.name",
                "CUSTOM.headline",
                "CUSTOM.offer_details",
                "GYM.timings",
                "GYM.support_phone",
                "GYM.google_maps_url"
        );
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.humanSupport("Chat with Reception", 1),
                TemplateButtonRecord.quickReply("Stop Notifications", "STOP", 2)
        );
    }
}
