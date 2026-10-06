package com.freelance.rest.jaxrs.filter;

import com.freelance.keys.authorization.permission.PermissionConfigKeys;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.endpoint.annotation.Anonymous;
import com.freelance.rest.app.endpoint.annotation.Authorization;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthorizationFilter Unit Tests")
class AuthorizationFilterTest {

    private AuthorizationFilter filter;

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
        Transaction.start();
        com.freelance.config.CacheRegistry.getInstance().init();
        filter = new AuthorizationFilter();
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {
        }
    }

    private void setResourceInfo(Class<?> clazz, String methodName) throws Exception {
        ResourceInfo resInfo = (ResourceInfo) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ResourceInfo.class},
                (proxy, method, args) -> {
                    if ("getResourceMethod".equals(method.getName())) {
                        return methodName != null ? clazz.getMethod(methodName) : null;
                    }
                    if ("getResourceClass".equals(method.getName())) {
                        return clazz;
                    }
                    return null;
                }
        );
        Field f = AuthorizationFilter.class.getDeclaredField("resourceInfo");
        f.setAccessible(true);
        f.set(filter, resInfo);
    }

    private ContainerRequestContext createMockRequestContext() {
        return (ContainerRequestContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ContainerRequestContext.class},
                (proxy, method, args) -> null
        );
    }

    @Test
    @DisplayName("filter throws ValidationException when resourceInfo is null and authorization is enabled")
    void testFilterNullResourceInfoAuthEnabledThrows() {
        CacheManager.put("cache_SystemConfig", PermissionConfigKeys.AUTHORIZATION_ENABLED.getKey(), "true");
        assertThrows(com.freelance.rest.app.service.exception.validation.ValidationException.class,
                () -> filter.filter(createMockRequestContext()));
    }

    @Test
    @DisplayName("filter allows request when resourceInfo is null and authorization is disabled")
    void testFilterNullResourceInfoAuthDisabled() {
        CacheManager.put("cache_SystemConfig", PermissionConfigKeys.AUTHORIZATION_ENABLED.getKey(), "false");
        assertDoesNotThrow(() -> filter.filter(createMockRequestContext()));
    }

    @Test
    @DisplayName("filter allows request when method is annotated with @Anonymous")
    void testMethodAnonymous() throws Exception {
        setResourceInfo(TestEndpoint.class, "anonymousMethod");
        assertDoesNotThrow(() -> filter.filter(createMockRequestContext()));
    }

    @Test
    @DisplayName("filter allows request when class is annotated with @Anonymous")
    void testClassAnonymous() throws Exception {
        setResourceInfo(TestAnonClassEndpoint.class, "regularMethod");
        assertDoesNotThrow(() -> filter.filter(createMockRequestContext()));
    }

    @Test
    @DisplayName("filter allows request when method is annotated with @Authenticated")
    void testMethodAuthenticated() throws Exception {
        CacheManager.put("cache_SystemConfig", PermissionConfigKeys.AUTHORIZATION_ENABLED.getKey(), "true");
        setResourceInfo(TestEndpoint.class, "authenticatedMethod");
        assertDoesNotThrow(() -> filter.filter(createMockRequestContext()));
    }

    @Test
    @DisplayName("filter allows request when class is annotated with @Authenticated")
    void testClassAuthenticated() throws Exception {
        CacheManager.put("cache_SystemConfig", PermissionConfigKeys.AUTHORIZATION_ENABLED.getKey(), "true");
        setResourceInfo(TestAuthClassEndpoint.class, "regularMethod");
        assertDoesNotThrow(() -> filter.filter(createMockRequestContext()));
    }

    @Test
    @DisplayName("filter returns early when authorization is disabled in configuration")
    void testAuthorizationDisabled() throws Exception {
        CacheManager.put("cache_SystemConfig", PermissionConfigKeys.AUTHORIZATION_ENABLED.getKey(), "false");
        setResourceInfo(TestEndpoint.class, "authorizedMethod");

        assertDoesNotThrow(() -> filter.filter(createMockRequestContext()));
        assertEquals("test.auth.key", Transaction.getRequestContext().authorizationKey());
    }

    private static class TestEndpoint {
        @Anonymous
        public void anonymousMethod() {}

        @com.freelance.rest.app.endpoint.annotation.Authenticated
        public void authenticatedMethod() {}

        @Authorization(key = "test.auth.key", name = "Test Auth", group = "Test Group", description = "Test Desc")
        public void authorizedMethod() {}

        public void regularMethod() {}
    }

    @Anonymous
    private static class TestAnonClassEndpoint {
        public void regularMethod() {}
    }

    @com.freelance.rest.app.endpoint.annotation.Authenticated
    private static class TestAuthClassEndpoint {
        public void regularMethod() {}
    }
}
