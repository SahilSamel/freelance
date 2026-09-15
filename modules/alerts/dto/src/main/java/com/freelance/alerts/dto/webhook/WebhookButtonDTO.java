package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookButtonDTO(
        String text,
        String payload
) {}
