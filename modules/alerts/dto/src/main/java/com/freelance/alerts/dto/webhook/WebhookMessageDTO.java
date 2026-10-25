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
        WebhookButtonDTO button,
        WebhookMediaDTO image,
        WebhookMediaDTO document,
        WebhookMediaDTO audio,
        WebhookMediaDTO video,
        String replyToId
) {
    public WebhookMessageDTO(
            String from,
            String id,
            String timestamp,
            String type,
            WebhookTextMessageDTO text,
            WebhookInteractiveDTO interactive,
            WebhookButtonDTO button,
            WebhookMediaDTO image,
            WebhookMediaDTO document,
            WebhookMediaDTO audio,
            String replyToId
    ) {
        this(from, id, timestamp, type, text, interactive, button, image, document, audio, null, replyToId);
    }

    public WebhookMessageDTO(
            String from,
            String id,
            String timestamp,
            String type,
            WebhookTextMessageDTO text,
            WebhookInteractiveDTO interactive,
            WebhookButtonDTO button,
            WebhookMediaDTO image,
            WebhookMediaDTO document,
            WebhookMediaDTO audio
    ) {
        this(from, id, timestamp, type, text, interactive, button, image, document, audio, null, null);
    }

    public WebhookMessageDTO(
            String from,
            String id,
            String timestamp,
            String type,
            WebhookTextMessageDTO text,
            WebhookInteractiveDTO interactive,
            WebhookButtonDTO button,
            WebhookMediaDTO image,
            WebhookMediaDTO document
    ) {
        this(from, id, timestamp, type, text, interactive, button, image, document, null, null, null);
    }

    public WebhookMessageDTO(
            String from,
            String id,
            String timestamp,
            String type,
            WebhookTextMessageDTO text,
            WebhookInteractiveDTO interactive,
            WebhookButtonDTO button
    ) {
        this(from, id, timestamp, type, text, interactive, button, null, null, null, null, null);
    }

    public WebhookMediaDTO extractMedia() {
        if (image != null) {
            return image;
        }
        if (document != null) {
            return document;
        }
        if (audio != null) {
            return audio;
        }
        if (video != null) {
            return video;
        }
        return null;
    }

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
        if (image != null) {
            if (image.caption() != null && !image.caption().isBlank()) {
                return image.caption();
            }
            return "[Image]";
        }
        if (document != null) {
            if (document.caption() != null && !document.caption().isBlank()) {
                return document.caption();
            }
            if (document.filename() != null && !document.filename().isBlank()) {
                return document.filename();
            }
            return "[Document]";
        }
        if (audio != null) {
            return "[Audio]";
        }
        if (video != null) {
            if (video.caption() != null && !video.caption().isBlank()) {
                return video.caption();
            }
            return "🎬 [Video/GIF]";
        }
        return type != null ? "[" + type + "]" : "[unknown]";
    }
}
