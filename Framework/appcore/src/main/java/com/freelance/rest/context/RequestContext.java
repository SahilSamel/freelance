package com.freelance.rest.context;

import java.util.UUID;

public record RequestContext(UUID userId, String sessionKey) {

    // 1. Replaces public RequestContext()
    public static RequestContext empty() {
        return new RequestContext(null, null);
    }

    // 2. Replaces public RequestContext(UUID userId)
    public static RequestContext of(UUID userId) {
        return new RequestContext(userId, null);
    }

    // 3. Replaces the string-to-UUID conversion logic
    public static RequestContext of(String userId) {
        if (userId == null || userId.isBlank()) {
            return new RequestContext(null, null);
        }
        return new RequestContext(UUID.fromString(userId), null);
    }

    // 4. Replaces setUserId(UUID) - returns a new record with the updated value
    public RequestContext withUserId(UUID userId) {
        return new RequestContext(userId, this.sessionKey);
    }

    // Replaces setUserId(String) - returns a new record with the updated value
    public RequestContext withUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return new RequestContext(null, this.sessionKey);
        }
        return new RequestContext(UUID.fromString(userId), this.sessionKey);
    }

    // 5. Replaces setSessionKey(String)
    public RequestContext withSessionKey(String sessionKey) {
        return new RequestContext(this.userId, sessionKey);
    }
}