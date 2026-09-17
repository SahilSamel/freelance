package com.freelance.auth.dto;

import java.time.Instant;

public record SupportGrantResponseDTO(
        boolean active,
        Instant expiresAt
) {
}
