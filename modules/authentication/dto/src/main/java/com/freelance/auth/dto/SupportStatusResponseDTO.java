package com.freelance.auth.dto;

import java.time.Instant;

public record SupportStatusResponseDTO(
        boolean active,
        Instant expiresAt,
        String pendingReason,
        String pendingTicketId
) {
}
