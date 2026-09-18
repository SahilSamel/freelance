package com.freelance.auth.session;

import com.freelance.auth.dto.*;
import com.freelance.cache.config.SessionCacheConfig;
import com.freelance.cache.config.SessionCacheEntry;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Session Unit Tests")
class SessionTest {

    private Session session;

    @BeforeEach
    void setUp() throws Exception {
        session = new Session();

        CacheManager mgr = CacheManager.getInstance();
        setField(mgr, "initialized", false);
        clearMap(mgr, "configsByName");
        clearMap(mgr, "nameByClass");
        clearMap(mgr, "caches");

        mgr.init("com.freelance.fixtures");

        java.lang.reflect.Constructor<?> constructor = Class.forName("com.freelance.rest.app.transaction.context.TransactionContext")
                .getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object[] args = new Object[constructor.getParameterCount()];
        Object ctx = constructor.newInstance(args);

        Field field = com.freelance.rest.app.transaction.Transaction.class.getDeclaredField("contextHolder");
        field.setAccessible(true);
        ((ThreadLocal<Object>) field.get(null)).set(ctx);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        Field field = com.freelance.rest.app.transaction.Transaction.class.getDeclaredField("contextHolder");
        field.setAccessible(true);
        ((ThreadLocal<?>) field.get(null)).remove();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void clearMap(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        ((Map<?, ?>) f.get(target)).clear();
    }

    @Test
    @DisplayName("generateSession creates session in cache with future expiration")
    void generateSession_createsSessionInCache() {
        UUID userId = UUID.randomUUID();
        SessionGenerateResponseDTO response = session.generateSession(new SessionGenerateRequestDTO(userId));

        assertNotNull(response);
        assertNotNull(response.sessionKey());

        SessionCacheEntry entry = CacheManager.getInstance().get(SessionCacheConfig.NAME, userId);
        assertNotNull(entry);
        assertTrue(entry.getSessions().containsKey(response.sessionKey()));
        assertTrue(entry.getSessions().get(response.sessionKey()) > System.currentTimeMillis());
    }

    @Test
    @DisplayName("verifySession slides expiration forward on active session")
    void verifySession_slidesExpiration() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        SessionGenerateResponseDTO gen = session.generateSession(new SessionGenerateRequestDTO(userId));
        String sessionKey = gen.sessionKey();

        SessionCacheEntry entry = CacheManager.getInstance().get(SessionCacheConfig.NAME, userId);
        long initialExpiry = entry.getSessions().get(sessionKey);

        Thread.sleep(50);

        session.verifySession(new SessionVerifyRequestDTO(sessionKey, userId));

        SessionCacheEntry updatedEntry = CacheManager.getInstance().get(SessionCacheConfig.NAME, userId);
        long slidExpiry = updatedEntry.getSessions().get(sessionKey);

        assertTrue(slidExpiry > initialExpiry, "Expiry should have slid forward after verifySession");
    }

    @Test
    @DisplayName("verifySession throws INVALID_SESSION when session key not found")
    void verifySession_throwsInvalidSessionWhenNotFound() {
        UUID userId = UUID.randomUUID();
        ConstraintException ex = assertThrows(
                ConstraintException.class,
                () -> session.verifySession(new SessionVerifyRequestDTO("unknown-key", userId))
        );
        assertEquals(SessionConstraintErrors.INVALID_SESSION.getErrorKey(), ex.getError().getErrorKey());
    }

    @Test
    @DisplayName("verifySession throws EXPIRED_SESSION when session is expired in cache")
    void verifySession_throwsExpiredSessionWhenExpired() {
        UUID userId = UUID.randomUUID();
        SessionGenerateResponseDTO gen = session.generateSession(new SessionGenerateRequestDTO(userId));
        String sessionKey = gen.sessionKey();

        // Expire session manually
        SessionCacheEntry entry = CacheManager.getInstance().get(SessionCacheConfig.NAME, userId);
        entry.getSessions().put(sessionKey, System.currentTimeMillis() - 1000L);
        CacheManager.getInstance().put(SessionCacheConfig.NAME, userId, entry);

        ConstraintException ex = assertThrows(
                ConstraintException.class,
                () -> session.verifySession(new SessionVerifyRequestDTO(sessionKey, userId))
        );
        assertEquals(SessionConstraintErrors.EXPIRED_SESSION.getErrorKey(), ex.getError().getErrorKey());
    }

    @Test
    @DisplayName("refreshSession rotates active session key and sets new expiration")
    void refreshSession_rotatesSessionKey() {
        UUID userId = UUID.randomUUID();
        SessionGenerateResponseDTO gen = session.generateSession(new SessionGenerateRequestDTO(userId));
        String oldKey = gen.sessionKey();

        SessionRefreshResponseDTO refreshed = session.refreshSession(new SessionRefreshRequestDTO(oldKey, userId));
        assertNotNull(refreshed);
        assertNotNull(refreshed.newSessionKey());
        assertNotEquals(oldKey, refreshed.newSessionKey());

        SessionCacheEntry entry = CacheManager.getInstance().get(SessionCacheConfig.NAME, userId);
        assertFalse(entry.getSessions().containsKey(oldKey), "Old session key must be removed");
        assertTrue(entry.getSessions().containsKey(refreshed.newSessionKey()), "New session key must exist in cache");
        assertTrue(entry.getSessions().get(refreshed.newSessionKey()) > System.currentTimeMillis());
    }

    @Test
    @DisplayName("refreshSession returns null when session is expired")
    void refreshSession_returnsNullWhenExpired() {
        UUID userId = UUID.randomUUID();
        SessionGenerateResponseDTO gen = session.generateSession(new SessionGenerateRequestDTO(userId));
        String sessionKey = gen.sessionKey();

        // Expire session manually
        SessionCacheEntry entry = CacheManager.getInstance().get(SessionCacheConfig.NAME, userId);
        entry.getSessions().put(sessionKey, System.currentTimeMillis() - 1000L);
        CacheManager.getInstance().put(SessionCacheConfig.NAME, userId, entry);

        SessionRefreshResponseDTO refreshed = session.refreshSession(new SessionRefreshRequestDTO(sessionKey, userId));
        assertNotNull(refreshed);
        assertNull(refreshed.newSessionKey());
    }

    @Test
    @DisplayName("invalidateSession removes session key from cache")
    void invalidateSession_removesSessionKey() {
        UUID userId = UUID.randomUUID();
        SessionGenerateResponseDTO gen = session.generateSession(new SessionGenerateRequestDTO(userId));
        String sessionKey = gen.sessionKey();

        SessionInvalidateResponseDTO inv = session.invalidateSession(new SessionInvalidateRequestDTO(sessionKey, userId));
        assertTrue(inv.success());

        SessionCacheEntry entry = CacheManager.getInstance().get(SessionCacheConfig.NAME, userId);
        assertNull(entry, "Cache entry should be removed when no sessions remain");
    }
}
