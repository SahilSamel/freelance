package com.freelance.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogCreateRequestDTO(
        Instant timestamp,
        String traceId,
        UUID userId,
        String httpMethod,
        String endpoint,
        int statusCode,
        long durationMs,
        String ipAddress,
        String userAgent,
        String authMechanism
) {
}
