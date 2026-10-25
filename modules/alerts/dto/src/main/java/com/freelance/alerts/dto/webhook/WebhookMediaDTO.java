package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookMediaDTO(
        String id,
        @JsonProperty("mime_type") @JsonAlias({"mimeType", "contentType"}) String mimeType,
        String sha256,
        String caption,
        String filename,
        @JsonProperty("link") @JsonAlias({"url"}) String link
) {
    public WebhookMediaDTO(String id, String mimeType, String sha256, String caption, String filename) {
        this(id, mimeType, sha256, caption, filename, null);
    }
}
