package com.freelance.tenant.dto;

import java.time.Instant;

public record TenantResponseDTO(
        String tenantId,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
