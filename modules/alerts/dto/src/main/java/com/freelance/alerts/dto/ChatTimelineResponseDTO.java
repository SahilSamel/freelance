package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatTimelineResponseDTO(
        UUID userId,
        boolean isHumanRequired,
        List<ChatMessageDTO> messages,
        String userName,
        String contactValue
) {
    public ChatTimelineResponseDTO(UUID userId, boolean isHumanRequired, List<ChatMessageDTO> messages) {
        this(userId, isHumanRequired, messages, null, null);
    }
}
