package com.freelance.alerts.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookUserPreferenceDTO(
        @JsonProperty("wa_id") String waId,
        String phone,
        String category,
        String action,
        String preference,
        String status,
        String timestamp
) {
    public String resolveWaId() {
        if (waId != null && !waId.isBlank()) return waId;
        return phone;
    }

    public boolean isOptOut() {
        String act = action != null ? action : (preference != null ? preference : status);
        if (act == null) return true;
        String upper = act.toUpperCase();
        return upper.contains("OPT_OUT") || upper.contains("STOP") || upper.contains("BLOCK") || upper.contains("DISABLE");
    }
}
