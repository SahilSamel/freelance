package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HumanSupportRequiredTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "human_support_required";

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Human Support Required";
    }

    @Override
    public String getCategory() {
        return "SUPPORT";
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }

    @Override
    public String getContent() {
        return "Customer {{USER.username}} is requesting human support assistance.";
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
        return Set.of("USER.username");
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
