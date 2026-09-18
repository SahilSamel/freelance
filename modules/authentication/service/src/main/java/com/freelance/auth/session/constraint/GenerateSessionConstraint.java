package com.freelance.auth.session.constraint;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.SessionGenerateRequestDTO;
import com.freelance.keys.authentication.auth.AuthConfigKeys;
import com.freelance.cache.config.SessionCacheConfig;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class GenerateSessionConstraint extends AbstractConstraint {

    public void checkConstraints(SessionGenerateRequestDTO request) {

        boolean allowConcurrent = ConfigurationEngine.getBoolean(AuthConfigKeys.CONCURRENT_LOGIN_ALLOWED);

        if (!allowConcurrent) {
            CacheManager.getInstance().invalidate(SessionCacheConfig.NAME, request.userId());
        }
    }
}
