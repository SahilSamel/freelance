package com.freelance.rest.context;

import java.util.UUID;

/**
 * Immutable per-request metadata container storing security and routing identity data.
 * <p>
 *     Meant to be wrapped in a threadLocal holder for restricting data access to currently executing thread.
 * </p>
 */
public record RequestContext(UUID userId, String sessionKey, String authorizationKey) {

    public static RequestContext empty() {
        return new RequestContext(null, null, null);
    }

    public void withUserId(UUID userId) {
        new RequestContext(userId, this.sessionKey, this.authorizationKey);
    }

    public void withSessionKey(String sessionKey) {
        new RequestContext(this.userId, sessionKey, this.authorizationKey);
    }

    public void withAuthorizationKey(String authorizationKey) {
        new RequestContext(this.userId, this.sessionKey, authorizationKey);
    }
}