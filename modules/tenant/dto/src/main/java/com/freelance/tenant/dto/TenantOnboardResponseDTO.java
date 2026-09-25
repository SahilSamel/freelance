package com.freelance.tenant.dto;

import java.time.Instant;

public record TenantOnboardResponseDTO(
        String tenantId,
        String name,
        String status,
        String publicKeyBase64,
        String day0Token,
        Instant expiresAt
) {}
