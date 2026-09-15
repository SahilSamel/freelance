package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookTemplateStatusDTO(
        String event,
        @JsonProperty("message_template_id") String messageTemplateId,
        @JsonProperty("message_template_name") String messageTemplateName,
        @JsonProperty("message_template_language") String messageTemplateLanguage,
        String reason
) {}