package com.freelance.auth.session.constraint;

import com.freelance.auth.dto.SessionVerifyRequestDTO;
import com.freelance.cache.config.SessionCacheConfig;
import com.freelance.cache.config.SessionCacheEntry;
import com.freelance.error.authentication.ConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.Map;

public class VerifySessionConstraint extends AbstractConstraint {

    public void checkConstraints(SessionVerifyRequestDTO request) {

        SessionCacheEntry sessionCache = CacheManager.getInstance().get(SessionCacheConfig.NAME, request.userId());

        if (sessionCache == null) {
            raiseConstraintError(ConstraintErrors.INVALID_SESSION);
            return;
        }

        Map<String, Long> sessions = sessionCache.getSessions();

        if (sessions == null) {
            raiseConstraintError(ConstraintErrors.INVALID_SESSION);
            return;
        }

        Long expiryTime = sessions.get(request.sessionKey());

        if (expiryTime == null) {
            raiseConstraintError(ConstraintErrors.INVALID_SESSION);
            return;
        }

        if (expiryTime <= System.currentTimeMillis()) {
            raiseConstraintError(ConstraintErrors.EXPIRED_SESSION);
        }
    }
}