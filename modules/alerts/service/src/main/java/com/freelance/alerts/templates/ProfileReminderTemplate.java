package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ProfileReminderTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "profile_incomplete_reminder";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Profile Incomplete Reminder";
    }

    @Override
    public String getCategory() {
        return "REMINDER";
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }

    @Override
    public String getContent() {
        return "Hi {{USER.first_name}}, please complete your profile details.";
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
        return AlertChannel.PUSH;
    }

    @Override
    public Set<String> getRequiredAttributes() {
        return Set.of("USER.first_name");
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return Collections.emptyList();
    }

    @Override
    public String getPriority() {
        return "NORMAL";
    }
}
