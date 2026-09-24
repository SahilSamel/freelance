package com.freelance.auth.session.constraint;

import com.freelance.auth.dto.SessionVerifyRequestDTO;
import com.freelance.cache.config.SessionCacheConfig;
import com.freelance.cache.config.SessionCacheEntry;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.Map;

public class VerifySessionConstraint extends AbstractConstraint {

    public void checkConstraints(SessionVerifyRequestDTO request) {

        SessionCacheEntry sessionCache = CacheManager.get(SessionCacheConfig.NAME, request.userId());

        if (sessionCache == null) {
            raiseConstraintError(SessionConstraintErrors.INVALID_SESSION);
            return;
        }

        Map<String, Long> sessions = sessionCache.getSessions();

        if (sessions == null) {
            raiseConstraintError(SessionConstraintErrors.INVALID_SESSION);
            return;
        }

        Long expiryTime = sessions.get(request.sessionKey());

        if (expiryTime == null) {
            raiseConstraintError(SessionConstraintErrors.INVALID_SESSION);
            return;
        }

        if (expiryTime <= System.currentTimeMillis()) {
            raiseConstraintError(SessionConstraintErrors.EXPIRED_SESSION);
        }
    }
}