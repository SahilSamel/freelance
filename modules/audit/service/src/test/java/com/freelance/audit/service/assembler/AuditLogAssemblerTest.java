package com.freelance.audit.service.assembler;

import com.freelance.audit.dto.AuditLogDTO;
import com.freelance.audit.dto.list.AuditLogListResponseDTO;
import com.freelance.audit.entity.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLogAssembler Tests")
class AuditLogAssemblerTest {

    private final AuditLogAssembler assembler = new AuditLogAssembler();

    @Test
    @DisplayName("toDTO maps all entity fields correctly")
    void toDTO_mapsFields() {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        AuditLog log = new AuditLog(
                now, "TRACE-123", userId, "POST", "/users/create",
                "{\"username\":\"john\"}", 201, 45L, "127.0.0.1", "Mozilla/5.0", "Bearer"
        );
        log.setId(id);

        AuditLogDTO dto = assembler.toDTO(log);
        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals(now, dto.timestamp());
        assertEquals("TRACE-123", dto.traceId());
        assertEquals(userId, dto.userId());
        assertEquals("POST", dto.httpMethod());
        assertEquals("/users/create", dto.endpoint());
        assertEquals("{\"username\":\"john\"}", dto.requestData());
        assertEquals(201, dto.statusCode());
        assertEquals(45L, dto.durationMs());
        assertEquals("127.0.0.1", dto.ipAddress());
    }

    @Test
    @DisplayName("toListResponse handles empty and populated lists")
    void toListResponse_handlesLists() {
        AuditLogListResponseDTO empty = assembler.toListResponse(Collections.emptyList());
        assertNotNull(empty);
        assertEquals(0, empty.totalCount());
        assertTrue(empty.auditLogs().isEmpty());

        AuditLog log = new AuditLog(
                Instant.now(), "T1", UUID.randomUUID(), "GET", "/test",
                200, 10L, "127.0.0.1", "agent", "Bearer"
        );
        log.setId(UUID.randomUUID());

        AuditLogListResponseDTO populated = assembler.toListResponse(List.of(log));
        assertNotNull(populated);
        assertEquals(1, populated.totalCount());
        assertEquals("T1", populated.auditLogs().get(0).traceId());
    }

    @Test
    @DisplayName("toEntity maps all request fields correctly including requestData")
    void toEntity_mapsFields() {
        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        com.freelance.audit.dto.AuditLogCreateRequestDTO req = new com.freelance.audit.dto.AuditLogCreateRequestDTO(
                now, "TRACE-999", userId, "POST", "/api/test", "{\"action\":\"submit\"}",
                200, 30L, "192.168.1.1", "curl/7.68", "api_key"
        );

        AuditLog entity = assembler.toEntity(req);
        assertNotNull(entity);
        assertEquals(now, entity.getTimestamp());
        assertEquals("TRACE-999", entity.getTraceId());
        assertEquals(userId, entity.getUserId());
        assertEquals("POST", entity.getHttpMethod());
        assertEquals("/api/test", entity.getEndpoint());
        assertEquals("{\"action\":\"submit\"}", entity.getRequestData());
        assertEquals(200, entity.getStatusCode());
        assertEquals(30L, entity.getDurationMs());
        assertEquals("192.168.1.1", entity.getIpAddress());
        assertEquals("curl/7.68", entity.getUserAgent());
        assertEquals("api_key", entity.getAuthMechanism());
    }
}
