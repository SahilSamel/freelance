package com.freelance.alerts.dto;

import java.util.List;
import java.util.UUID;

public record ChatTimelineResponseDTO(
        UUID userId,
        boolean isHumanRequired,
        List<ChatMessageDTO> messages
) {}
