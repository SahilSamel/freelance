package com.freelance.audit.service;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.audit.dto.list.AuditLogListResponseDTO;
import com.freelance.audit.service.assembler.AuditLogAssembler;
import com.freelance.audit.entity.AuditLog;
import com.freelance.fixtures.FakeAuditLogRepository;
import com.freelance.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditService List Tests")
class AuditServiceListTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("listAuditLogs returns all logs when no filter is provided")
    void listAuditLogs_noFilter_returnsAll() {
        FakeAuditLogRepository repo = new FakeAuditLogRepository();
        repo.storage.add(new AuditLog(Instant.now(), "T1", UUID.randomUUID(), "POST", "/users", 201, 20L, "127.0.0.1", "test", "token"));
        repo.storage.add(new AuditLog(Instant.now(), "T2", UUID.randomUUID(), "GET", "/roles", 200, 15L, "127.0.0.1", "test", "token"));

        AuditService service = new AuditService(repo, new AuditLogAssembler());
        AuditLogListResponseDTO response = service.listAuditLogs(AuditLogListRequestDTO.empty());

        assertNotNull(response);
        assertEquals(2, response.totalCount());
        assertEquals(2, response.auditLogs().size());
    }

    @Test
    @DisplayName("listAuditLogs filters by HTTP method and status code")
    void listAuditLogs_filtersByMethodAndStatus() {
        FakeAuditLogRepository repo = new FakeAuditLogRepository();
        repo.storage.add(new AuditLog(Instant.now(), "T1", UUID.randomUUID(), "POST", "/users", 201, 20L, "127.0.0.1", "test", "token"));
        repo.storage.add(new AuditLog(Instant.now(), "T2", UUID.randomUUID(), "GET", "/roles", 200, 15L, "127.0.0.1", "test", "token"));

        AuditService service = new AuditService(repo, new AuditLogAssembler());
        AuditLogListResponseDTO response = service.listAuditLogs(
                new AuditLogListRequestDTO(null, null, "POST", null, 201, null, null, null, null)
        );

        assertNotNull(response);
        assertEquals(1, response.totalCount());
        assertEquals("T1", response.auditLogs().get(0).traceId());
    }

    @Test
    @DisplayName("createAuditLog persists log when auditing is enabled")
    void createAuditLog_whenAuditingEnabled_persists() {
        FakeAuditLogRepository repo = new FakeAuditLogRepository();
        AuditService service = new AuditService(repo, new AuditLogAssembler());

        com.freelance.audit.dto.AuditLogCreateRequestDTO req = new com.freelance.audit.dto.AuditLogCreateRequestDTO(
                Instant.now(), "T100", UUID.randomUUID(), "POST", "/test", "{\"key\":\"val\"}",
                200, 10L, "127.0.0.1", "agent", "Bearer"
        );

        var res = service.createAuditLog(req);
        assertNotNull(res);
        assertEquals(1, repo.storage.size());
        assertEquals("T100", repo.storage.get(0).getTraceId());
    }

    @Test
    @DisplayName("createAuditLog returns null and skips persisting when auditing is disabled")
    void createAuditLog_whenAuditingDisabled_skips() {
        com.freelance.manager.CacheManager.put("cache_SystemConfig", "audit.enabled", "false");

        FakeAuditLogRepository repo = new FakeAuditLogRepository();
        AuditService service = new AuditService(repo, new AuditLogAssembler());

        com.freelance.audit.dto.AuditLogCreateRequestDTO req = new com.freelance.audit.dto.AuditLogCreateRequestDTO(
                Instant.now(), "T101", UUID.randomUUID(), "POST", "/test", null,
                200, 10L, "127.0.0.1", "agent", "Bearer"
        );

        var res = service.createAuditLog(req);
        assertNull(res);
        assertTrue(repo.storage.isEmpty());
    }
}
