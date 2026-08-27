package com.freelance.cache.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache entry holding an authenticated user's active session keys mapped to their expiry timestamps.
 */
public class SessionCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Long> sessions = new HashMap<>();

    public SessionCacheEntry() {
    }

    public SessionCacheEntry(String initialEntry, long expiryMillis) {
        if (initialEntry != null) {
            this.sessions.put(initialEntry, expiryMillis);
        }
    }

    public Map<String, Long> getSessions() {
        return sessions;
    }

    public void setSessions(Map<String, Long> sessions) {
        this.sessions = sessions;
    }

    public void clearEntries() {
        if (this.sessions != null) {
            this.sessions.clear();
        }
    }
}