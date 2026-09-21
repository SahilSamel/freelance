package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SupportAccessRequestedTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "support_access_requested";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Support Access Requested";
    }

    @Override
    public String getCategory() {
        return TemplateCategory.AUTHENTICATION.name();
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }

    @Override
    public String getContent() {
        return "Support staff has requested temporary administrative access to your account.";
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
        return Collections.emptySet();
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return Collections.emptyList();
    }

    @Override
    public String getPriority() {
        return "HIGH";
    }
}
