package com.freelance.alerts.dto;

import java.io.Serializable;
import java.util.UUID;

public record AlertEventPayloadDTO(
        UUID templateId,
        UUID userId,
        String rawContent,
        UUID messageId
) implements Serializable {

    public AlertEventPayloadDTO(UUID templateId, UUID userId) {
        this(templateId, userId, null, null);
    }

    public AlertEventPayloadDTO(UUID templateId, UUID userId, String rawContent) {
        this(templateId, userId, rawContent, null);
    }
}
