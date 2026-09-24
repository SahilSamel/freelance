package com.freelance.rest.jaxrs.filter;

import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantFilter Unit Tests")
class TenantFilterTest {

    private TenantFilter tenantFilter;
    private TransactionFilter transactionFilter;
    private Map<String, Object> requestProperties;
    private Map<String, String> headers;
    private String requestPath;
    private URI baseUri;

    @BeforeEach
    void setUp() {
        tenantFilter = new TenantFilter();
        transactionFilter = new TransactionFilter();
        requestProperties = new HashMap<>();
        headers = new HashMap<>();
        requestPath = "users/read";
        baseUri = URI.create("http://localhost:8080/");

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
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {
        }
    }

    private ContainerRequestContext createMockRequestContext() {
        UriInfo mockUriInfo = (UriInfo) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{UriInfo.class},
                (proxy, method, args) -> {
                    if ("getPath".equals(method.getName())) return requestPath;
                    if ("getBaseUri".equals(method.getName())) return baseUri;
                    return null;
                }
        );

        return (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getUriInfo".equals(name)) return mockUriInfo;
                    if ("getHeaderString".equals(name)) {
                        return headers.get(args[0]);
                    }
                    if ("setProperty".equals(name)) {
                        requestProperties.put((String) args[0], args[1]);
                        return null;
                    }
                    if ("getProperty".equals(name)) {
                        return requestProperties.get((String) args[0]);
                    }
                    return null;
                }
        );
    }

    @Test
    @DisplayName("Should resolve tenantId from X-Tenant-ID header")
    void testResolveFromHeader() {
        headers.put(TenantFilter.TENANT_HEADER, "acme_corp");
        ContainerRequestContext ctx = createMockRequestContext();

        tenantFilter.filter(ctx);

        assertEquals("acme_corp", requestProperties.get(TenantFilter.TENANT_PROPERTY));
    }

    @Test
    @DisplayName("Should resolve tenantId from host subdomain when header is absent")
    void testResolveFromSubdomain() {
        headers.put("Host", "globex.api.example.com:8080");
        ContainerRequestContext ctx = createMockRequestContext();

        tenantFilter.filter(ctx);

        assertEquals("globex", requestProperties.get(TenantFilter.TENANT_PROPERTY));
    }

    @Test
    @DisplayName("Should fallback to default tenant when host is localhost and header is absent")
    void testFallbackForLocalhost() {
        headers.put("Host", "localhost:8080");
        ContainerRequestContext ctx = createMockRequestContext();

        tenantFilter.filter(ctx);

        assertEquals(TenantFilter.DEFAULT_TENANT, requestProperties.get(TenantFilter.TENANT_PROPERTY));
    }

    @Test
    @DisplayName("Should use default tenant for excluded system paths")
    void testExcludedPaths() {
        requestPath = "coverage";
        ContainerRequestContext ctx = createMockRequestContext();

        tenantFilter.filter(ctx);

        assertEquals(TenantFilter.DEFAULT_TENANT, requestProperties.get(TenantFilter.TENANT_PROPERTY));
    }

    @Test
    @DisplayName("Should reject invalid tenant format with 400 Bad Request")
    void testInvalidTenantFormat() {
        headers.put(TenantFilter.TENANT_HEADER, "invalid!@#");
        ContainerRequestContext ctx = createMockRequestContext();

        WebApplicationException ex = assertThrows(WebApplicationException.class, () -> tenantFilter.filter(ctx));
        assertEquals(400, ex.getResponse().getStatus());
    }

    @Test
    @DisplayName("TransactionFilter should copy tenantId from request property to Transaction.getRequestContext()")
    void testTransactionFilterContextPropagation() {
        headers.put(TenantFilter.TENANT_HEADER, "tenant_omega");
        ContainerRequestContext ctx = createMockRequestContext();

        tenantFilter.filter(ctx);
        transactionFilter.filter(ctx);

        assertEquals("tenant_omega", Transaction.getRequestContext().tenantId());
    }
}
