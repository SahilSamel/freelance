package com.freelance.cache.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SessionCacheEntry Unit Tests")
class SessionCacheEntryTest {

    @Test
    @DisplayName("Default constructor creates empty sessions map")
    void testDefaultConstructor() {
        SessionCacheEntry entry = new SessionCacheEntry();
        assertNotNull(entry.getSessions());
        assertTrue(entry.getSessions().isEmpty());
    }

    @Test
    @DisplayName("Parametric constructor initializes session entry properly")
    void testParametricConstructor() {
        long expiry = System.currentTimeMillis() + 10000;
        SessionCacheEntry entry = new SessionCacheEntry("session-123", expiry);
        assertEquals(1, entry.getSessions().size());
        assertEquals(expiry, entry.getSessions().get("session-123"));

        SessionCacheEntry nullEntry = new SessionCacheEntry(null, expiry);
        assertTrue(nullEntry.getSessions().isEmpty());
    }

    @Test
    @DisplayName("setSessions and clearEntries modify session map")
    void testSetSessionsAndClear() {
        SessionCacheEntry entry = new SessionCacheEntry();
        Map<String, Long> map = new HashMap<>();
        map.put("s1", 1000L);
        map.put("s2", 2000L);

        entry.setSessions(map);
        assertEquals(2, entry.getSessions().size());

        entry.clearEntries();
        assertTrue(entry.getSessions().isEmpty());

        entry.setSessions(null);
        assertDoesNotThrow(entry::clearEntries);
    }
}
