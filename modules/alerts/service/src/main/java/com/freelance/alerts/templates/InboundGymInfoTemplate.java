package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class InboundGymInfoTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "inbound_gym_info";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Inbound Gym Hours & Info";
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
        return "{{GYM.name}} Facility Details: 📍\n\n"
                + "• Timings: {{GYM.timings}}\n"
                + "• Front Desk: {{GYM.support_phone}}\n"
                + "• Address: {{GYM.address}}\n"
                + "• Google Maps: {{GYM.google_maps_url}}\n"
                + "• Community Channel: {{GYM.whatsapp_channel_url}}\n\n"
                + "Join our WhatsApp Community Channel for daily workout schedules, challenges, and gym announcements!";
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
        return Set.of("GYM.name", "GYM.timings", "GYM.support_phone", "GYM.address", "GYM.google_maps_url", "GYM.whatsapp_channel_url");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.quickReplyWithFollowup("Main Menu", "INBOUND_WELCOME_MENU", InboundWelcomeTemplate.TEMPLATE_KEY, 1),
                TemplateButtonRecord.humanSupport("Chat with Reception", 2)
        );
    }
}
