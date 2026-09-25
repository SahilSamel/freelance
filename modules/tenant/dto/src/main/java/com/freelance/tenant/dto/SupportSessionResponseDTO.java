package com.freelance.tenant.dto;

import java.time.Instant;

public record SupportSessionResponseDTO(
        String tenantId,
        String vendorToken,
        Instant expiresAt
) {}
