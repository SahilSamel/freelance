package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class MemberLapsedWinbackTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "member_lapsed_winback";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Previous Member Offer";
    }

    @Override
    public String getCategory() {
        return TemplateCategory.MARKETING.name();
    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public String getContent() {
        return "Hey {{USER.first_name}}, the floor at *{{GYM.name}}* isn't the same without you! 💪\n\n"
                + "Ready to get back into your routine? Restart with us this week to unlock a *special returning member discount* along with additional renewal perks waiting for you at the desk.\n\n"
                + "📍 Location: {{GYM.google_maps_url}}\n"
                + "⏰ Timings: {{GYM.timings}}\n"
                + "📞 Front Desk: {{GYM.support_phone}}\n\n"
                + "Tap *Claim Offer* below to see what we have lined up for your comeback!\n\n"
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
    public Set<String> getRequiredAttributes() {
        return Set.of(
                "USER.first_name",
                "GYM.name",
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
