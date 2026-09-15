package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookErrorDTO(
        int code,
        String title,
        String message,
        @JsonProperty("error_subcode") Integer errorSubcode,
        @JsonProperty("error_data") WebhookErrorDataDTO errorData
) {}