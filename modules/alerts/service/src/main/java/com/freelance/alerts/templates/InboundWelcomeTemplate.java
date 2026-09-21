package com.freelance.alerts.templates;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.ButtonType;
import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.ITemplate;
import com.freelance.alerts.template.ITemplateButton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InboundWelcomeTemplate implements ITemplate {

    public static final String TEMPLATE_KEY = "inbound_welcome_menu";

    public record WelcomeButton(
            String text,
            ButtonType type,
            String payload,
            String replyTemplateKey,
            boolean requiresHumanAgent,
            int sequenceOrder
    ) implements ITemplateButton {
        @Override public String getText() { return text; }
        @Override public ButtonType getType() { return type; }
        @Override public String getPayload() { return payload; }
        @Override public String getReplyTemplateKey() { return replyTemplateKey; }
        @Override public boolean requiresHumanAgent() { return requiresHumanAgent; }
        @Override public int getSequenceOrder() { return sequenceOrder; }
    }

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public String getName() {
        return "Inbound Welcome Menu";
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
        return "Hello! How can we assist you today? Please select an option below:";
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
        return List.of(
                new WelcomeButton("Need Human Support", ButtonType.QUICK_REPLY, "WELCOME_HUMAN", null, true, 1)
        );
    }
}
