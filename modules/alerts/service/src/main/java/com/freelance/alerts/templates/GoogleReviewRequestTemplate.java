package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class GoogleReviewRequestTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "google_review_request";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Member Feedback & Google Review";
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
        return "Hey {{USER.first_name}}, loved having you crush your workouts at *{{GYM.name}}*! 💪\n\n"
                + "Got 30 seconds? Drop us a quick rating on Google—your feedback helps local fitness lovers find us and keeps our floor thriving.\n\n"
                + "⭐ *Rate us here:* {{GYM.google_maps_url}}\n\n"
                + "If there's anything we can improve for your next session, just tap below to let us know directly!";
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
        return Set.of("USER.first_name", "GYM.name", "GYM.google_maps_url");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.humanSupport("Chat with Reception", 1)
        );
    }
}
