package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;
import java.util.UUID;

public record AdminChatReplyRequestDTO(
        UUID userId,
        AlertChannel channel,
        String message
) {}
