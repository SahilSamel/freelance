package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.freelance.alerts.AlertChannel;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatReadRequestDTO(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("channel") AlertChannel channel
) {
    public ChatReadRequestDTO(UUID userId) {
        this(userId, AlertChannel.WHATSAPP);
    }
}
