package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayloadDTO(
        String object,
        List<WebhookEntryDTO> entry,
        // Flat MSG91 Webhook & DLR properties
        @JsonProperty("request_id") String requestId,
        String status,
        String sender,
        String recipient,
        String message,
        @JsonProperty("msg_id") String msgId,
        String timestamp
) {
    public WhatsAppWebhookPayloadDTO(String object, List<WebhookEntryDTO> entry) {
        this(object, entry, null, null, null, null, null, null, null);
    }

    public List<WebhookEntryDTO> resolveEntries() {
        if (entry != null && !entry.isEmpty()) {
            return entry;
        }

        // Handle MSG91 flat status / delivery reports
        if (requestId != null && status != null) {
            WebhookStatusDTO statusDto = new WebhookStatusDTO(requestId, status, timestamp, recipient, null);
            WebhookValueDTO val = new WebhookValueDTO("whatsapp", null, List.of(statusDto), null, null);
            WebhookChangeDTO change = new WebhookChangeDTO("messages", val);
            return List.of(new WebhookEntryDTO(requestId, List.of(change)));
        }

        // Handle MSG91 flat inbound messages
        if (sender != null && message != null) {
            WebhookTextMessageDTO txt = new WebhookTextMessageDTO(message);
            String id = msgId != null ? msgId : ("msg91_" + System.currentTimeMillis());
            WebhookMessageDTO msg = new WebhookMessageDTO(sender, id, timestamp, "text", txt, null, null);
            WebhookValueDTO val = new WebhookValueDTO("whatsapp", null, null, List.of(msg), null);
            WebhookChangeDTO change = new WebhookChangeDTO("messages", val);
            return List.of(new WebhookEntryDTO(sender, List.of(change)));
        }

        return Collections.emptyList();
    }
}
