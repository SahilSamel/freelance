package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;
import java.util.UUID;

public record ChatTimelineRequestDTO(
        UUID userId,
        AlertChannel channel,
        Integer limit
) {}
