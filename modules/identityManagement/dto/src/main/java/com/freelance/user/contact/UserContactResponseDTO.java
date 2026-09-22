package com.freelance.user.contact;

import com.freelance.alerts.AlertChannel;

import java.util.UUID;

/**
 * Response DTO representing persisted user contact information.
 */
public record UserContactResponseDTO(
        UUID id,
        UUID userId,
        AlertChannel channel,
        String contactValue,
        boolean isOptedOut,
        boolean isActive
) {
}
