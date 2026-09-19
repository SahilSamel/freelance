package com.freelance.audit.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Audit DTO Unit Tests")
class AuditLogDtoTest {

    @Test
    @DisplayName("AuditLogDTO secondary constructor sets null requestData")
    void testAuditLogDtoConstructors() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();

        AuditLogDTO dto1 = new AuditLogDTO(
                id, now, "trace-1", userId, "tenant-1", "GET", "/api/test",
                "req-data", 200, 50L, "127.0.0.1", "JUnit", "Bearer"
        );
        assertEquals("req-data", dto1.requestData());
        assertEquals("tenant-1", dto1.tenantId());

        AuditLogDTO dto2 = new AuditLogDTO(
                id, now, "trace-1", userId, "tenant-1", "GET", "/api/test",
                200, 50L, "127.0.0.1", "JUnit", "Bearer"
        );
        assertNull(dto2.requestData());
        assertEquals(id, dto2.id());
        assertEquals(now, dto2.timestamp());
        assertEquals("trace-1", dto2.traceId());
        assertEquals(userId, dto2.userId());
        assertEquals(200, dto2.statusCode());
        assertEquals(50L, dto2.durationMs());
        assertEquals("127.0.0.1", dto2.ipAddress());
        assertEquals("JUnit", dto2.userAgent());
        assertEquals("Bearer", dto2.authMechanism());
    }

    @Test
    @DisplayName("AuditLogCreateRequestDTO secondary constructor sets null requestData")
    void testAuditLogCreateRequestDtoConstructors() {
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();

        AuditLogCreateRequestDTO req1 = new AuditLogCreateRequestDTO(
                now, "trace-2", userId, "POST", "/api/save",
                "json-body", 201, 100L, "192.168.1.1", "Mozilla", "ApiKey"
        );
        assertEquals("json-body", req1.requestData());

        AuditLogCreateRequestDTO req2 = new AuditLogCreateRequestDTO(
                now, "trace-2", userId, "POST", "/api/save",
                201, 100L, "192.168.1.1", "Mozilla", "ApiKey"
        );
        assertNull(req2.requestData());
        assertEquals(now, req2.timestamp());
        assertEquals("trace-2", req2.traceId());
        assertEquals(userId, req2.userId());
        assertEquals("POST", req2.httpMethod());
        assertEquals("/api/save", req2.endpoint());
        assertEquals(201, req2.statusCode());
        assertEquals(100L, req2.durationMs());
        assertEquals("192.168.1.1", req2.ipAddress());
        assertEquals("Mozilla", req2.userAgent());
        assertEquals("ApiKey", req2.authMechanism());
    }

    @Test
    @DisplayName("AuditLogCreateResponseDTO record components")
    void testAuditLogCreateResponseDto() {
        UUID id = UUID.randomUUID();
        AuditLogCreateResponseDTO resp = new AuditLogCreateResponseDTO(id, "trace-abc");
        assertEquals(id, resp.id());
        assertEquals("trace-abc", resp.traceId());
    }
}
