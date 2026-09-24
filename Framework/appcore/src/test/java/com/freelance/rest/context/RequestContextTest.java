package com.freelance.rest.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

    @Test
    @DisplayName("empty() should create a RequestContext with all null fields")
    void testEmptyRequestContext() {
        RequestContext context = RequestContext.empty();
        assertNull(context.userId());
        assertNull(context.sessionKey());
        assertNull(context.authorizationKey());
        assertNull(context.tenantId());
    }

    @Test
    @DisplayName("3-arg constructor should initialize 3 fields and default tenantId to null")
    void testThreeArgConstructor() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, "session-123", "AUTH_KEY");
        assertEquals(userId, context.userId());
        assertEquals("session-123", context.sessionKey());
        assertEquals("AUTH_KEY", context.authorizationKey());
        assertNull(context.tenantId());
    }

    @Test
    @DisplayName("4-arg constructor should initialize all fields including tenantId")
    void testFourArgConstructor() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, "session-123", "AUTH_KEY", "tenant-acme");
        assertEquals(userId, context.userId());
        assertEquals("session-123", context.sessionKey());
        assertEquals("AUTH_KEY", context.authorizationKey());
        assertEquals("tenant-acme", context.tenantId());
    }

    @Test
    @DisplayName("Mutator methods should update fields correctly")
    void testMutators() {
        RequestContext context = RequestContext.empty();
        UUID userId = UUID.randomUUID();

        context.withUserId(userId);
        context.withSessionKey("new-session");
        context.withAuthorizationKey("NEW_AUTH");
        context.withTenantId("tenant-globex");

        assertEquals(userId, context.userId());
        assertEquals("new-session", context.sessionKey());
        assertEquals("NEW_AUTH", context.authorizationKey());
        assertEquals("tenant-globex", context.tenantId());
    }
}
