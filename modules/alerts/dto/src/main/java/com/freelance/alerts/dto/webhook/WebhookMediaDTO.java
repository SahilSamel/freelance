package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookMediaDTO(
        String id,
        @JsonProperty("mime_type") String mimeType,
        String sha256,
        String caption,
        String filename
) {}
