package com.freelance.cache.config;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RoleAuthorizationKeyCacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<UUID> authorizationKeys = new HashSet<>();

    public RoleAuthorizationKeyCacheEntry(){}

    public RoleAuthorizationKeyCacheEntry(Set<UUID> authorizationKeys){
        this.authorizationKeys.addAll(authorizationKeys);
    }

    public Set<UUID> getAuthorizationKeys() {
        return authorizationKeys;
    }

    public void setAuthorizationKeys(Set<UUID> authorizationKeys) {
        this.authorizationKeys = authorizationKeys;
    }
}
