package com.freelance.alerts.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public record AlertEventPayloadDTO(
        UUID templateId,
        UUID userId,
        String rawContent,
        UUID messageId,
        String mediaUrl,
        String mediaId,
        String mediaType,
        Map<String, String> customParameters
) implements Serializable {

    public AlertEventPayloadDTO(UUID templateId, UUID userId) {
        this(templateId, userId, null, null, null, null, null, null);
    }

    public AlertEventPayloadDTO(UUID templateId, UUID userId, String rawContent) {
        this(templateId, userId, rawContent, null, null, null, null, null);
    }

    public AlertEventPayloadDTO(UUID templateId, UUID userId, String rawContent, UUID messageId) {
        this(templateId, userId, rawContent, messageId, null, null, null, null);
    }

    public AlertEventPayloadDTO(
            UUID templateId,
            UUID userId,
            String rawContent,
            UUID messageId,
            String mediaUrl,
            String mediaId,
            String mediaType
    ) {
        this(templateId, userId, rawContent, messageId, mediaUrl, mediaId, mediaType, null);
    }
}
