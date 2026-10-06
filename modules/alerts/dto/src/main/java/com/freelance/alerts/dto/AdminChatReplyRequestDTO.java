package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freelance.alerts.AlertChannel;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminChatReplyRequestDTO(
        UUID userId,
        AlertChannel channel,
        String message,
        String mediaUrl,
        String mediaId,
        String mediaType
) {
    public AdminChatReplyRequestDTO(UUID userId, AlertChannel channel, String message) {
        this(userId, channel, message, null, null, null);
    }
}
