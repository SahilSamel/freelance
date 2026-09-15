package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayloadDTO(
        String object,
        List<WebhookEntryDTO> entry
) {}
