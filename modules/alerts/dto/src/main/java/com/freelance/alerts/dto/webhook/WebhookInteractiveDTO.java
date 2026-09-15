package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookInteractiveDTO(
        String type,
        @JsonProperty("button_reply") WebhookButtonReplyDTO buttonReply,
        @JsonProperty("list_reply") WebhookListReplyDTO listReply
) {
    public WebhookInteractiveDTO(String type, WebhookButtonReplyDTO buttonReply) {
        this(type, buttonReply, null);
    }
}