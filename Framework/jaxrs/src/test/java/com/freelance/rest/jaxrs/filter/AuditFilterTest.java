package com.freelance.rest.jaxrs.filter;

import com.freelance.audit.service.AuditService;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditFilter Ingress, Egress & Sanitization Unit Tests")
class AuditFilterTest {

    private final AuditFilter auditFilter = new AuditFilter();

    @BeforeEach
    void setUp() {
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
                    if ("getTransaction".equals(method.getName())) return mockTx;
                    return null;
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
        com.freelance.config.CacheRegistry.getInstance().init();
    }

    @Test
    @DisplayName("sanitizes password in JSON payload")
    void sanitizeRequestData_masksPasswordInJson() {
        String json = "{\"username\": \"john\", \"password\": \"Secret123!\"}";
        String sanitized = auditFilter.sanitizeRequestData(json);

        assertNotNull(sanitized);
        assertTrue(sanitized.contains("\"password\": \"******\""));
        assertFalse(sanitized.contains("Secret123!"));
        assertTrue(sanitized.contains("\"username\": \"john\""));
    }

    @Test
    @DisplayName("sanitizes token and secret in JSON payload")
    void sanitizeRequestData_masksTokenAndSecret() {
        String json = "{\"token\": \"eyJhbGciOi...\", \"secret\": \"my-app-secret\", \"name\": \"test\"}";
        String sanitized = auditFilter.sanitizeRequestData(json);

        assertNotNull(sanitized);
        assertTrue(sanitized.contains("\"token\": \"******\""));
        assertTrue(sanitized.contains("\"secret\": \"******\""));
        assertFalse(sanitized.contains("eyJhbGciOi..."));
        assertFalse(sanitized.contains("my-app-secret"));
        assertTrue(sanitized.contains("\"name\": \"test\""));
    }

    @Test
    @DisplayName("sanitizes query parameters with credentials")
    void sanitizeRequestData_masksQueryParameters() {
        String query = "query: page=1&password=myPass&token=abc12345&limit=20";
        String sanitized = auditFilter.sanitizeRequestData(query);

        assertNotNull(sanitized);
        assertTrue(sanitized.contains("password=******"));
        assertTrue(sanitized.contains("token=******"));
        assertFalse(sanitized.contains("myPass"));
        assertFalse(sanitized.contains("abc12345"));
        assertTrue(sanitized.contains("page=1"));
        assertTrue(sanitized.contains("limit=20"));
    }

    @Test
    @DisplayName("returns null or blank gracefully when data is empty")
    void sanitizeRequestData_handlesNullAndBlank() {
        assertNull(auditFilter.sanitizeRequestData(null));
        assertEquals("", auditFilter.sanitizeRequestData(""));
        assertEquals("   ", auditFilter.sanitizeRequestData("   "));
    }

    @Test
    @DisplayName("Ingress filter: extracts headers, IP, auth mechanism and entity stream")
    void testIngressFilter() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Trace-ID", "custom-trace-123");
        headers.put("X-Forwarded-For", "198.51.100.1, 198.51.100.2");
        headers.put("Authorization", "Bearer jwt-token-xyz");
        headers.put("User-Agent", "Mozilla/5.0 IntegrationTest");

        byte[] requestBody = "{\"username\":\"alice\",\"password\":\"secret\"}".getBytes(StandardCharsets.UTF_8);
        InputStream[] streamHolder = new InputStream[]{new ByteArrayInputStream(requestBody)};

        UriInfo mockUriInfo = (UriInfo) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{UriInfo.class},
                (proxy, method, args) -> {
                    if ("getPath".equals(method.getName())) return "auth/login";
                    if ("getRequestUri".equals(method.getName())) return URI.create("http://localhost:8080/auth/login?param=1");
                    return null;
                }
        );

        ContainerRequestContext reqCtx = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeaderString": return headers.get(args[0]);
                        case "setProperty": properties.put((String) args[0], args[1]); return null;
                        case "getProperty": return properties.get(args[0]);
                        case "hasEntity": return true;
                        case "getEntityStream": return streamHolder[0];
                        case "setEntityStream": streamHolder[0] = (InputStream) args[0]; return null;
                        case "getMethod": return "POST";
                        case "getUriInfo": return mockUriInfo;
                        default: return null;
                    }
                }
        );

        auditFilter.filter(reqCtx);

        assertEquals("custom-trace-123", properties.get("audit.traceId"));
        assertEquals("198.51.100.1", properties.get("audit.ipAddress"));
        assertEquals("bearer", properties.get("audit.authMechanism"));
        assertEquals("Mozilla/5.0 IntegrationTest", properties.get("audit.userAgent"));
        assertNotNull(properties.get("audit.requestData"));
        assertTrue(((String) properties.get("audit.requestData")).contains("\"password\":\"******\""));

        // Test egress filter
        MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        ContainerResponseContext resCtx = (ContainerResponseContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerResponseContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeaders": return responseHeaders;
                        case "getStatus": return 200;
                        default: return null;
                    }
                }
        );

        auditFilter.filter(reqCtx, resCtx);
        assertEquals("custom-trace-123", responseHeaders.getFirst("X-Trace-ID"));
    }

    @Test
    @DisplayName("Ingress filter auth mechanism variations: ApiKey, Basic, Custom, X-API-Key, Anonymous, None")
    void testAuthMechanisms() throws Exception {
        testAuthHeader("ApiKey key123", null, false, "api_key");
        testAuthHeader("Basic user:pass", null, false, "basic");
        testAuthHeader("Custom scheme", null, false, "custom");
        testAuthHeader(null, "x-key-value", false, "api_key");
        testAuthHeader(null, null, true, "anonymous");
        testAuthHeader(null, null, false, "none");
    }

    private void testAuthHeader(String authHeader, String xApiKey, boolean isAnon, String expectedMechanism) throws Exception {
        AuditFilter filter = new AuditFilter();
        Map<String, Object> properties = new HashMap<>();

        if (isAnon) {
            ResourceInfo resInfo = (ResourceInfo) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResourceInfo.class},
                    (proxy, method, args) -> {
                        if ("getResourceMethod".equals(method.getName())) return DummyEndpoint.class.getMethod("anonMethod");
                        if ("getResourceClass".equals(method.getName())) return DummyEndpoint.class;
                        return null;
                    }
            );
            Field f = AuditFilter.class.getDeclaredField("resourceInfo");
            f.setAccessible(true);
            f.set(filter, resInfo);
        }

        ContainerRequestContext reqCtx = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeaderString":
                            if ("Authorization".equals(args[0])) return authHeader;
                            if ("X-API-Key".equals(args[0])) return xApiKey;
                            if ("X-Real-IP".equals(args[0])) return "10.0.0.5";
                            return null;
                        case "setProperty": properties.put((String) args[0], args[1]); return null;
                        case "getProperty": return properties.get(args[0]);
                        case "hasEntity": return false;
                        case "getMethod": return "GET";
                        case "getUriInfo": return null;
                        default: return null;
                    }
                }
        );

        filter.filter(reqCtx);
        assertEquals(expectedMechanism, properties.get("audit.authMechanism"));
        assertEquals("10.0.0.5", properties.get("audit.ipAddress"));
    }

    @Test
    @DisplayName("Auditing disabled returns early on ingress and egress")
    void testAuditingDisabled() {
        String key = com.freelance.keys.audit.http.HttpAuditConfigKeys.AUDITING_ENABLED.getKey();
        com.freelance.manager.CacheManager.put("cache_SystemConfig", key, "false");
        try {
            Map<String, Object> properties = new HashMap<>();
            ContainerRequestContext reqCtx = (ContainerRequestContext) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ContainerRequestContext.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getHeaderString": return null;
                            case "setProperty": properties.put((String) args[0], args[1]); return null;
                            case "getProperty": return properties.get(args[0]);
                            case "hasEntity": return false;
                            default: return null;
                        }
                    }
            );

            auditFilter.filter(reqCtx);
            assertNull(properties.get("audit.startTime"));

            MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
            ContainerResponseContext resCtx = (ContainerResponseContext) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ContainerResponseContext.class},
                    (proxy, method, args) -> {
                        if ("getHeaders".equals(method.getName())) return responseHeaders;
                        return null;
                    }
            );

            auditFilter.filter(reqCtx, resCtx);
            assertNotNull(responseHeaders.getFirst("X-Trace-ID"));
        } finally {
            com.freelance.manager.CacheManager.put("cache_SystemConfig", key, "true");
        }
    }

    @Test
    @DisplayName("Truncates payloads longer than 10000 characters")
    void testPayloadTruncation() {
        String hugePayload = "a".repeat(10500);
        String sanitized = auditFilter.sanitizeRequestData(hugePayload);
        assertEquals(10500, sanitized.length());

        Map<String, Object> properties = new HashMap<>();
        InputStream stream = new ByteArrayInputStream(hugePayload.getBytes(StandardCharsets.UTF_8));
        ContainerRequestContext reqCtx = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "hasEntity": return true;
                        case "getEntityStream": return stream;
                        case "setEntityStream": return null;
                        case "getHeaderString": return null;
                        case "setProperty": properties.put((String) args[0], args[1]); return null;
                        case "getProperty": return properties.get(args[0]);
                        case "getMethod": return "POST";
                        case "getUriInfo": return null;
                        default: return null;
                    }
                }
        );

        auditFilter.filter(reqCtx);
        String data = (String) properties.get("audit.requestData");
        assertNotNull(data);
        assertTrue(data.endsWith("... [TRUNCATED]"));
        assertEquals(10000 + "... [TRUNCATED]".length(), data.length());
    }

    @Test
    @DisplayName("Handles stream read exception gracefully")
    void testStreamReadException() {
        Map<String, Object> properties = new HashMap<>();
        InputStream badStream = new InputStream() {
            @Override
            public int read() throws java.io.IOException {
                throw new java.io.IOException("Stream error");
            }
        };

        ContainerRequestContext reqCtx = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "hasEntity": return true;
                        case "getEntityStream": return badStream;
                        case "getHeaderString": return null;
                        case "setProperty": properties.put((String) args[0], args[1]); return null;
                        case "getProperty": return properties.get(args[0]);
                        case "getMethod": return "POST";
                        case "getUriInfo": return null;
                        default: return null;
                    }
                }
        );

        assertDoesNotThrow(() -> auditFilter.filter(reqCtx));
        assertNull(properties.get("audit.requestData"));
    }

    @Test
    @DisplayName("Class-level Anonymous annotation detection")
    void testClassLevelAnonymous() throws Exception {
        AuditFilter filter = new AuditFilter();
        ResourceInfo resInfo = (ResourceInfo) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ResourceInfo.class},
                (proxy, method, args) -> {
                    if ("getResourceMethod".equals(method.getName())) return DummyAnonClass.class.getMethod("regularMethod");
                    if ("getResourceClass".equals(method.getName())) return DummyAnonClass.class;
                    return null;
                }
        );
        Field f = AuditFilter.class.getDeclaredField("resourceInfo");
        f.setAccessible(true);
        f.set(filter, resInfo);

        Map<String, Object> properties = new HashMap<>();
        ContainerRequestContext reqCtx = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeaderString": return null;
                        case "setProperty": properties.put((String) args[0], args[1]); return null;
                        case "getProperty": return properties.get(args[0]);
                        case "hasEntity": return false;
                        case "getMethod": return "GET";
                        case "getUriInfo": return null;
                        default: return null;
                    }
                }
        );

        filter.filter(reqCtx);
        assertEquals("anonymous", properties.get("audit.authMechanism"));
    }

    @Test
    @DisplayName("Tests auth mechanisms: Basic, Custom, and X-API-Key")
    void testVariousAuthMechanisms() {
        AuditFilter filter = new AuditFilter();

        // 1. Basic auth
        Map<String, Object> props1 = new HashMap<>();
        ContainerRequestContext req1 = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeaderString": return "Basic dXNlcjpwYXNz";
                        case "setProperty": props1.put((String) args[0], args[1]); return null;
                        case "getProperty": return props1.get(args[0]);
                        case "hasEntity": return false;
                        case "getMethod": return "GET";
                        default: return null;
                    }
                }
        );
        filter.filter(req1);
        assertEquals("basic", props1.get("audit.authMechanism"));

        // 2. Custom auth
        Map<String, Object> props2 = new HashMap<>();
        ContainerRequestContext req2 = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeaderString": return "TokenXYZ 12345";
                        case "setProperty": props2.put((String) args[0], args[1]); return null;
                        case "getProperty": return props2.get(args[0]);
                        case "hasEntity": return false;
                        case "getMethod": return "GET";
                        default: return null;
                    }
                }
        );
        filter.filter(req2);
        assertEquals("custom", props2.get("audit.authMechanism"));

        // 3. X-API-Key header
        Map<String, Object> props3 = new HashMap<>();
        ContainerRequestContext req3 = (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getHeaderString": return "X-API-Key".equals(args[0]) ? "api-secret-key" : null;
                        case "setProperty": props3.put((String) args[0], args[1]); return null;
                        case "getProperty": return props3.get(args[0]);
                        case "hasEntity": return false;
                        case "getMethod": return "GET";
                        default: return null;
                    }
                }
        );
        filter.filter(req3);
        assertEquals("api_key", props3.get("audit.authMechanism"));
    }

    @Anonymous
    private static class DummyAnonClass {
        public void regularMethod() {}
    }

    private static class DummyEndpoint {
        @Anonymous
        public void anonMethod() {}
    }
}
