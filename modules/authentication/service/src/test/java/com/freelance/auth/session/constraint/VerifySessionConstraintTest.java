package com.freelance.auth.session.constraint;

import com.freelance.auth.dto.SessionVerifyRequestDTO;
import com.freelance.cache.config.SessionCacheConfig;
import com.freelance.cache.config.SessionCacheEntry;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VerifySessionConstraint Unit Tests")
class VerifySessionConstraintTest {

    private VerifySessionConstraint constraint;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        constraint = new VerifySessionConstraint();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Valid active session passes constraint check")
    void testValidActiveSession() {
        UUID userId = UUID.randomUUID();
        String sessionKey = "active-key-1";

        SessionCacheEntry entry = new SessionCacheEntry(sessionKey, System.currentTimeMillis() + 60000);
        CacheManager.put(SessionCacheConfig.NAME, userId, entry);

        SessionVerifyRequestDTO req = new SessionVerifyRequestDTO(sessionKey, userId);
        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    @DisplayName("Throws INVALID_SESSION when no session cache entry exists for user")
    void testNoSessionEntryForUser() {
        UUID userId = UUID.randomUUID();
        SessionVerifyRequestDTO req = new SessionVerifyRequestDTO("missing-key", userId);

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(SessionConstraintErrors.INVALID_SESSION, ex.getError());
    }

    @Test
    @DisplayName("Throws INVALID_SESSION when sessions map is null")
    void testNullSessionsMap() {
        UUID userId = UUID.randomUUID();
        SessionCacheEntry entry = new SessionCacheEntry();
        entry.setSessions(null);
        CacheManager.put(SessionCacheConfig.NAME, userId, entry);

        SessionVerifyRequestDTO req = new SessionVerifyRequestDTO("key-1", userId);
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(SessionConstraintErrors.INVALID_SESSION, ex.getError());
    }

    @Test
    @DisplayName("Throws INVALID_SESSION when sessionKey is not present in sessions map")
    void testSessionKeyNotFound() {
        UUID userId = UUID.randomUUID();
        SessionCacheEntry entry = new SessionCacheEntry();
        entry.setSessions(new HashMap<>());
        CacheManager.put(SessionCacheConfig.NAME, userId, entry);

        SessionVerifyRequestDTO req = new SessionVerifyRequestDTO("unknown-key", userId);
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(SessionConstraintErrors.INVALID_SESSION, ex.getError());
    }

    @Test
    @DisplayName("Throws EXPIRED_SESSION when session expiry time is in the past")
    void testExpiredSession() {
        UUID userId = UUID.randomUUID();
        String sessionKey = "expired-key";

        SessionCacheEntry entry = new SessionCacheEntry(sessionKey, System.currentTimeMillis() - 5000);
        CacheManager.put(SessionCacheConfig.NAME, userId, entry);

        SessionVerifyRequestDTO req = new SessionVerifyRequestDTO(sessionKey, userId);
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
        assertEquals(SessionConstraintErrors.EXPIRED_SESSION, ex.getError());
    }
}
