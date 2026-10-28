package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;
import com.freelance.alerts.template.TemplateButtonRecord;

import java.util.List;
import java.util.Set;

public class PlanRenewalReminderTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "plan_renewal_reminder";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Plan Expiry Reminder";
    }

    @Override
    public String getCategory() {
        return TemplateCategory.UTILITY.name();
    }

    @Override
    public String getLanguage() {
        return "en";
    }

    @Override
    public String getContent() {
        return "Hey {{USER.first_name}}, a quick heads-up from *{{GYM.name}}*! ⏳\n\n"
                + "Your current membership is coming up for renewal soon:\n\n"
                + "🏋️ *Plan:* {{PLAN.name}}\n"
                + "📅 *Expires on:* {{PLAN.expiry_date}} ({{PLAN.days_left}} days left)\n\n"
                + "Drop by the front desk or tap below to check renewal options and keep your workout streak going without a break!";
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
        return Set.of("USER.first_name", "GYM.name", "PLAN.name", "PLAN.expiry_date", "PLAN.days_left");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return List.of(
                TemplateButtonRecord.humanSupport("Chat with Reception", 1)
        );
    }
}
