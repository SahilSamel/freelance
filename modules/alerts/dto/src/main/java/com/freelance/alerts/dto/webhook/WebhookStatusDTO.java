package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookStatusDTO(
        String id,
        String status,
        String timestamp,
        @JsonProperty("recipient_id") String recipientId,
        List<WebhookErrorDTO> errors
) {}