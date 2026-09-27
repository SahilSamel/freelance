package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;

import java.io.Serializable;
import java.util.UUID;

public record UserContactDTO(
        UUID id,
        UUID userId,
        AlertChannel channel,
        String contactValue,
        boolean isOptedOut,
        boolean isActive
) implements Serializable {
}
