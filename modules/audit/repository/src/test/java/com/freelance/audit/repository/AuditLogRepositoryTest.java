package com.freelance.audit.repository;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.audit.entity.AuditLog;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLogRepository & AuditLog Entity Unit Tests")
public class AuditLogRepositoryTest {

    private String lastJpql;
    private final Map<String, Object> lastParams = new HashMap<>();
    private int lastMaxResults = -1;
    private int lastFirstResult = -1;

    @BeforeEach
    void setUp() {
        lastJpql = null;
        lastParams.clear();
        lastMaxResults = -1;
        lastFirstResult = -1;

        EntityTransaction mockTx = (EntityTransaction) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    if ("isActive".equals(method.getName())) return true;
                    return null;
                }
        );

        EntityManager mockEm = (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTransaction": return mockTx;
                        case "createQuery":
                            lastJpql = (String) args[0];
                            return createMockQuery();
                        case "close": return null;
                        default: return null;
                    }
                }
        );

        EntityManagerFactory mockEmf = (EntityManagerFactory) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("createEntityManager".equals(method.getName())) return mockEm;
                    return null;
                }
        );

        Transaction.init(mockEmf);
    }

    @SuppressWarnings("unchecked")
    private TypedQuery<AuditLog> createMockQuery() {
        return (TypedQuery<AuditLog>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TypedQuery.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setParameter":
                            lastParams.put((String) args[0], args[1]);
                            return proxy;
                        case "setMaxResults":
                            lastMaxResults = (Integer) args[0];
                            return proxy;
                        case "setFirstResult":
                            lastFirstResult = (Integer) args[0];
                            return proxy;
                        case "getResultList":
                            return List.of(new AuditLog(Instant.now(), "trace-1", UUID.randomUUID(), "POST", "/users/create", 201, 15L, "127.0.0.1", "curl", "bearer"));
                        default: return null;
                    }
                }
        );
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Should build query with all filter parameters")
    void testFindAuditLogsWithAllFilters() {
        try (var tx = Transaction.start()) {
            AuditLogRepository repo = new AuditLogRepository();

            UUID userId = UUID.randomUUID();
            Instant from = Instant.now().minusSeconds(3600);
            Instant to = Instant.now();

            AuditLogListRequestDTO filter = new AuditLogListRequestDTO(
                    userId,
                    "trace-999",
                    "post",
                    "/auth/login",
                    200,
                    from,
                    to,
                    25,
                    50
            );

            List<AuditLog> results = repo.findAuditLogs(filter);
            assertNotNull(results);
            assertEquals(1, results.size());

            assertNotNull(lastJpql);
            assertTrue(lastJpql.contains("a.userId = :userId"));
            assertTrue(lastJpql.contains("a.traceId = :traceId"));
            assertTrue(lastJpql.contains("a.httpMethod = :httpMethod"));
            assertTrue(lastJpql.contains("a.endpoint LIKE :endpoint"));
            assertTrue(lastJpql.contains("a.statusCode = :statusCode"));
            assertTrue(lastJpql.contains("a.timestamp >= :fromTimestamp"));
            assertTrue(lastJpql.contains("a.timestamp <= :toTimestamp"));
            assertTrue(lastJpql.contains("ORDER BY a.timestamp DESC"));

            assertEquals(userId, lastParams.get("userId"));
            assertEquals("trace-999", lastParams.get("traceId"));
            assertEquals("POST", lastParams.get("httpMethod"));
            assertEquals("%/auth/login%", lastParams.get("endpoint"));
            assertEquals(200, lastParams.get("statusCode"));
            assertEquals(from, lastParams.get("fromTimestamp"));
            assertEquals(to, lastParams.get("toTimestamp"));

            assertEquals(25, lastMaxResults);
            assertEquals(50, lastFirstResult);
        }
    }

    @Test
    @DisplayName("Should build default query when filter is null")
    void testFindAllAndDefaultFilter() {
        try (var tx = Transaction.start()) {
            AuditLogRepository repo = new AuditLogRepository();

            List<AuditLog> results = repo.findAll();
            assertNotNull(results);
            assertEquals(1, results.size());

            assertEquals("SELECT a FROM AuditLog a WHERE 1=1 ORDER BY a.timestamp DESC", lastJpql);
            assertEquals(50, lastMaxResults);
            assertEquals(0, lastFirstResult);
        }
    }

    @Test
    @DisplayName("AuditLog entity constructors, prePersist, and getters/setters")
    void testAuditLogEntity() throws Exception {
        AuditLog log1 = new AuditLog();
        assertNull(log1.getId());
        assertNull(log1.getTimestamp());

        // Invoke @PrePersist onPersistAudit
        var method = AuditLog.class.getDeclaredMethod("onPersistAudit");
        method.setAccessible(true);
        method.invoke(log1);

        assertNotNull(log1.getId());
        assertNotNull(log1.getTimestamp());

        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        UUID customId = UUID.randomUUID();

        AuditLog log2 = new AuditLog(
                now, "trace-abc", userId, "GET", "/status",
                "req-payload", 200, 42L, "192.168.1.1", "Mozilla/5.0", "bearer"
        );

        assertEquals(now, log2.getTimestamp());
        assertEquals("trace-abc", log2.getTraceId());
        assertEquals(userId, log2.getUserId());
        assertEquals("GET", log2.getHttpMethod());
        assertEquals("/status", log2.getEndpoint());
        assertEquals("req-payload", log2.getRequestData());
        assertEquals(200, log2.getStatusCode());
        assertEquals(42L, log2.getDurationMs());
        assertEquals("192.168.1.1", log2.getIpAddress());
        assertEquals("Mozilla/5.0", log2.getUserAgent());
        assertEquals("bearer", log2.getAuthMechanism());

        // Setters
        log2.setId(customId);
        log2.setTimestamp(now.plusSeconds(60));
        log2.setTraceId("trace-new");
        log2.setUserId(null);
        log2.setHttpMethod("PUT");
        log2.setEndpoint("/new-endpoint");
        log2.setRequestData("new-payload");
        log2.setStatusCode(204);
        log2.setDurationMs(100L);
        log2.setIpAddress("10.0.0.1");
        log2.setUserAgent("CustomAgent");
        log2.setAuthMechanism("api_key");

        assertEquals(customId, log2.getId());
        assertEquals(now.plusSeconds(60), log2.getTimestamp());
        assertEquals("trace-new", log2.getTraceId());
        assertNull(log2.getUserId());
        assertEquals("PUT", log2.getHttpMethod());
        assertEquals("/new-endpoint", log2.getEndpoint());
        assertEquals("new-payload", log2.getRequestData());
        assertEquals(204, log2.getStatusCode());
        assertEquals(100L, log2.getDurationMs());
        assertEquals("10.0.0.1", log2.getIpAddress());
        assertEquals("CustomAgent", log2.getUserAgent());
        assertEquals("api_key", log2.getAuthMechanism());
    }
}
