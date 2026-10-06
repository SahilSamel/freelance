package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessageDTO(
        Integer sequenceNumber,
        UUID id,
        UUID userId,
        AlertChannel channel,
        AlertDirection direction,
        String messageType,
        String content,
        List<TemplateButtonDTO> buttons,
        AlertMessageStatus status,
        Instant timestamp,
        String mediaUrl,
        String mediaId,
        String mediaType
) {
    public ChatMessageDTO(
            UUID id,
            UUID userId,
            AlertChannel channel,
            AlertDirection direction,
            String messageType,
            String content,
            AlertMessageStatus status,
            Instant timestamp
    ) {
        this(null, id, userId, channel, direction, messageType, content, null, status, timestamp, null, null, null);
    }

    public ChatMessageDTO(
            UUID id,
            UUID userId,
            AlertChannel channel,
            AlertDirection direction,
            String messageType,
            String content,
            AlertMessageStatus status,
            Instant timestamp,
            String mediaUrl,
            String mediaId,
            String mediaType
    ) {
        this(null, id, userId, channel, direction, messageType, content, null, status, timestamp, mediaUrl, mediaId, mediaType);
    }

    public ChatMessageDTO(
            UUID id,
            UUID userId,
            AlertChannel channel,
            AlertDirection direction,
            String messageType,
            String content,
            List<TemplateButtonDTO> buttons,
            AlertMessageStatus status,
            Instant timestamp
    ) {
        this(null, id, userId, channel, direction, messageType, content, buttons, status, timestamp, null, null, null);
    }

    public ChatMessageDTO(
            Integer sequenceNumber,
            UUID id,
            UUID userId,
            AlertChannel channel,
            AlertDirection direction,
            String messageType,
            String content,
            AlertMessageStatus status,
            Instant timestamp
    ) {
        this(sequenceNumber, id, userId, channel, direction, messageType, content, null, status, timestamp, null, null, null);
    }

    public ChatMessageDTO(
            Integer sequenceNumber,
            UUID id,
            UUID userId,
            AlertChannel channel,
            AlertDirection direction,
            String messageType,
            String content,
            List<TemplateButtonDTO> buttons,
            AlertMessageStatus status,
            Instant timestamp
    ) {
        this(sequenceNumber, id, userId, channel, direction, messageType, content, buttons, status, timestamp, null, null, null);
    }
}
