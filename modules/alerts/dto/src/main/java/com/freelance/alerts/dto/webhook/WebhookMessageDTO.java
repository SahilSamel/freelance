package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookMessageDTO(
        String from,
        String id,
        String timestamp,
        String type,
        WebhookTextMessageDTO text,
        WebhookInteractiveDTO interactive,
        WebhookButtonDTO button
) {
    public String extractButtonPayload() {
        if (interactive != null) {
            if (interactive.buttonReply() != null) {
                return interactive.buttonReply().id();
            }
            if (interactive.listReply() != null) {
                return interactive.listReply().id();
            }
        }
        if (button != null) {
            return button.payload();
        }
        return null;
    }

    public String extractTextContent() {
        if (text != null && text.body() != null) {
            return text.body();
        }
        if (interactive != null) {
            if (interactive.buttonReply() != null && interactive.buttonReply().title() != null) {
                return interactive.buttonReply().title();
            }
            if (interactive.listReply() != null && interactive.listReply().title() != null) {
                return interactive.listReply().title();
            }
        }
        if (button != null && button.text() != null) {
            return button.text();
        }
        return type != null ? "[" + type + "]" : "[unknown]";
    }
}
