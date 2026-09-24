package com.freelance.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDTO(
        UUID id,
        Instant timestamp,
        String traceId,
        UUID userId,
        String httpMethod,
        String endpoint,
        String requestData,
        int statusCode,
        long durationMs,
        String ipAddress,
        String userAgent,
        String authMechanism
) {
    public AuditLogDTO(
            UUID id,
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
        this(id, timestamp, traceId, userId, httpMethod, endpoint, null, statusCode, durationMs, ipAddress, userAgent, authMechanism);
    }
}
