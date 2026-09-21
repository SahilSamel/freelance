package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;
import java.util.UUID;

public record ChatTimelineRequestDTO(
        UUID userId,
        AlertChannel channel,
        Integer limit,
        Boolean unreadOnly
) {
    public ChatTimelineRequestDTO(UUID userId, AlertChannel channel, Integer limit) {
        this(userId, channel, limit, false);
    }
}
