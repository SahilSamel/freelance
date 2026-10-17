package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class CampaignSeasonalOfferTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "campaign_seasonal_offer";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Seasonal & Holiday Offer";
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
        return "Hey {{USER.first_name}}, new season, new goals at {{GYM.name}}! ⚡\n\n"
                + "Make the most of this season with our limited-time pass:\n\n"
                + "*{{CUSTOM.headline}}*\n"
                + "{{CUSTOM.offer_details}}\n\n"
                + "📍 Location: {{GYM.google_maps_url}}\n"
                + "⏰ Timings: {{GYM.timings}}\n"
                + "📞 Front Desk: {{GYM.support_phone}}\n\n"
                + "Tap *Claim Offer* below to lock in your spot before the seasonal batch fills up!\n\n"
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
    public com.freelance.alerts.template.DispatchScope getDispatchScope() {
        return com.freelance.alerts.template.DispatchScope.BOTH;
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
                TemplateButtonRecord.humanSupport("Claim Offer", 1),
                TemplateButtonRecord.quickReply("Stop Notifications", "STOP", 2)
        );
    }
}
