package com.freelance.rest.context;

import java.util.UUID;

/**
 * Immutable per-request metadata container storing security and routing identity data.
 * <p>
 *     Meant to be wrapped in a threadLocal holder for restricting data access to currently executing thread.
 * </p>
 */
public class RequestContext {

    private UUID userId;
    private String sessionKey;
    private String authorizationKey;

    public RequestContext(UUID userId, String sessionKey, String authorizationKey) {
        this.userId = userId;
        this.sessionKey = sessionKey;
        this.authorizationKey = authorizationKey;
    }

    public static RequestContext empty() {
        return new RequestContext(null, null, null);
    }

    public UUID userId() {
        return userId;
    }

    public String sessionKey() {
        return sessionKey;
    }

    public String authorizationKey() {
        return authorizationKey;
    }

    public void withUserId(UUID userId) {
        this.userId = userId;
    }

    public void withSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void withAuthorizationKey(String authorizationKey) {
        this.authorizationKey = authorizationKey;
    }
}