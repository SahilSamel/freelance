package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;
import java.util.List;
import java.util.UUID;

public record NotificationReadRequestDTO(
        UUID userId,
        List<UUID> messageIds,
        AlertChannel channel
) {
    public NotificationReadRequestDTO(UUID userId) {
        this(userId, null, AlertChannel.PUSH);
    }

    public NotificationReadRequestDTO(UUID userId, List<UUID> messageIds) {
        this(userId, messageIds, AlertChannel.PUSH);
    }
}
