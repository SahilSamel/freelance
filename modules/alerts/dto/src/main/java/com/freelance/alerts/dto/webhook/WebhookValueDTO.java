package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookValueDTO(
        @JsonProperty("messaging_product") String messagingProduct,
        WebhookMetadataDTO metadata,
        List<WebhookStatusDTO> statuses,
        List<WebhookMessageDTO> messages,
        @JsonProperty("message_template_status_update") WebhookTemplateStatusDTO templateStatusUpdate,
        @JsonProperty("user_preferences") List<WebhookUserPreferenceDTO> userPreferences,
        String event,
        @JsonProperty("message_template_id") String messageTemplateId,
        @JsonProperty("message_template_name") String messageTemplateName,
        @JsonProperty("message_template_language") String messageTemplateLanguage,
        String reason
) {
    public WebhookValueDTO(
            String messagingProduct,
            WebhookMetadataDTO metadata,
            List<WebhookStatusDTO> statuses,
            List<WebhookMessageDTO> messages,
            WebhookTemplateStatusDTO templateStatusUpdate
    ) {
        this(messagingProduct, metadata, statuses, messages, templateStatusUpdate, null, null, null, null, null, null);
    }

    public WebhookTemplateStatusDTO resolveTemplateStatusUpdate() {
        if (templateStatusUpdate != null) {
            return templateStatusUpdate;
        }
        if (event != null || messageTemplateName != null || messageTemplateId != null) {
            return new WebhookTemplateStatusDTO(
                    event,
                    messageTemplateId,
                    messageTemplateName,
                    messageTemplateLanguage,
                    reason
            );
        }
        return null;
    }
}