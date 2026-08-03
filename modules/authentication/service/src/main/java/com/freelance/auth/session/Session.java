package com.freelance.auth.session;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.*;
import com.freelance.auth.session.constraint.GenerateSessionConstraint;
import com.freelance.auth.session.constraint.VerifySessionConstraint;
import com.freelance.auth.session.validation.GenerateSessionValidation;
import com.freelance.auth.session.validation.VerifySessionValidation;
import com.freelance.authentication.AuthenticationConfigKeys;
import com.freelance.cache.config.SessionCacheConfig;
import com.freelance.cache.config.SessionCacheEntry;
import com.freelance.manager.CacheManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Session {

    private long getExpiryMillis() {
        long ttlSeconds = Long.parseLong(ConfigurationEngine.get(AuthenticationConfigKeys.SESSION_TTL_SECONDS.getKey()));
        return System.currentTimeMillis() + (ttlSeconds * 1000L);
    }

    public SessionGenerateResponseDTO generateSession(SessionGenerateRequestDTO request) {

        new GenerateSessionValidation().validateRequest(request);
        new GenerateSessionConstraint().checkConstraints(request);

        String sessionKey = UUID.randomUUID().toString();
        long expiresAt = getExpiryMillis();

        SessionCacheEntry sessionCache = CacheManager.getInstance().get(SessionCacheConfig.NAME, request.userId());
        if (sessionCache == null) {
            sessionCache = new SessionCacheEntry();
        }

        Map<String, Long> keys = sessionCache.getSessions();
        if (keys == null) {
            keys = new HashMap<>();
        }

        synchronized (keys) {
            long now = System.currentTimeMillis();
            keys.entrySet().removeIf(entry -> entry.getValue() <= now);

            keys.put(sessionKey, expiresAt);
        }

        sessionCache.setSessions(keys);
        CacheManager.getInstance().put(SessionCacheConfig.NAME, request.userId(), sessionCache);

        return new SessionGenerateResponseDTO(sessionKey);
    }

    public void verifySession(SessionVerifyRequestDTO request) {
        new VerifySessionValidation().validateRequest(request);
        new VerifySessionConstraint().checkConstraints(request);
    }

    public SessionInvalidateResponseDTO invalidateSession(SessionInvalidateRequestDTO request) {
        if (request != null && request.sessionKey() != null && request.userId() != null) {

            SessionCacheEntry sessionCache = CacheManager.getInstance().get(SessionCacheConfig.NAME, request.userId());

            if (sessionCache != null) {
                Map<String, Long> keys = sessionCache.getSessions();
                if (keys != null && !keys.isEmpty()) {
                    synchronized (keys) {
                        keys.remove(request.sessionKey());
                    }

                    if (keys.isEmpty()) {
                        CacheManager.getInstance().invalidate(SessionCacheConfig.NAME, request.userId());
                    } else {
                        sessionCache.setSessions(keys);
                        CacheManager.getInstance().put(SessionCacheConfig.NAME, request.userId(), sessionCache);
                    }
                }
            }
        }

        return new SessionInvalidateResponseDTO(true);
    }

    public SessionRefreshResponseDTO refreshSession(SessionRefreshRequestDTO request) {

        SessionCacheEntry sessionCache = CacheManager.getInstance().get(SessionCacheConfig.NAME, request.userId());

        if (sessionCache != null) {
            Map<String, Long> keys = sessionCache.getSessions();
            if (keys != null && keys.containsKey(request.sessionKey())) {
                String newSessionKey = UUID.randomUUID().toString();
                long newExpiresAt = getExpiryMillis();

                synchronized (keys) {
                    keys.remove(request.sessionKey());  // old key is dead immediately
                    keys.put(newSessionKey, newExpiresAt);
                }

                sessionCache.setSessions(keys);
                CacheManager.getInstance().put(SessionCacheConfig.NAME, request.userId(), sessionCache);

                return new SessionRefreshResponseDTO(newSessionKey);
            }
        }

        return new SessionRefreshResponseDTO(null);
    }
}