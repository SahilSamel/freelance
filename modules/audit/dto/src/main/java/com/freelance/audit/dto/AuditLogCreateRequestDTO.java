package com.freelance.audit.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Request payload containing all intercepted audit event data.
 */
public record AuditLogCreateRequestDTO(
        Instant timestamp,
        String traceId,
        UUID userId,
        String tenantId,
        String httpMethod,
        String endpoint,
        int statusCode,
        long durationMs,
        String ipAddress,
        String userAgent,
        String authMechanism
) {
}
