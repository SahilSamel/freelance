package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HelloWorldTemplate implements ITemplate {

    @Override
    public String getTemplateKey() {
        return "hello_world";
    }

    @Override
    public String getName() {
        return "Hello World";
    }

    @Override
    public String getCategory() {
        return "UTILITY";
    }

    @Override
    public String getLanguage() {
        return "en_US";
    }

    @Override
    public String getContent() {
        return "Hello World";
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
        return Collections.emptySet();
    }

    @Override
    public List<ITemplateButton> getButtons() {
        return Collections.emptyList();
    }
}
