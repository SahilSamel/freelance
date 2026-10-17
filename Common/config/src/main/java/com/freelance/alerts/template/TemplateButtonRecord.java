package com.freelance.alerts.template;

import com.freelance.alerts.ButtonType;

/**
 * Standard immutable record implementation of {@link ITemplateButton}.
 */
public record TemplateButtonRecord(
        String text,
        ButtonType type,
        String payload,
        String replyTemplateKey,
        boolean requiresHumanAgent,
        int sequenceOrder
) implements ITemplateButton {

    public static TemplateButtonRecord quickReply(String text, String payload, int sequenceOrder) {
        return new TemplateButtonRecord(text, ButtonType.QUICK_REPLY, payload, null, false, sequenceOrder);
    }

    public static TemplateButtonRecord quickReplyWithFollowup(String text, String payload, String replyTemplateKey, int sequenceOrder) {
        return new TemplateButtonRecord(text, ButtonType.QUICK_REPLY, payload, replyTemplateKey, false, sequenceOrder);
    }

    public static TemplateButtonRecord humanSupport(String text, int sequenceOrder) {
        return new TemplateButtonRecord(text, ButtonType.QUICK_REPLY, "WELCOME_HUMAN", null, true, sequenceOrder);
    }

    public static TemplateButtonRecord urlButton(String text, String url, int sequenceOrder) {
        return new TemplateButtonRecord(text, ButtonType.URL, url, null, false, sequenceOrder);
    }

    @Override public String getText() { return text; }
    @Override public ButtonType getType() { return type; }
    @Override public String getPayload() { return payload; }
    @Override public String getReplyTemplateKey() { return replyTemplateKey; }
    @Override public boolean requiresHumanAgent() { return requiresHumanAgent; }
    @Override public int getSequenceOrder() { return sequenceOrder; }
}
