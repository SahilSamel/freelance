package com.freelance.cache.config;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RoleAuthorizationKeyCacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> authorizationKeys = new HashSet<>();

    public RoleAuthorizationKeyCacheEntry(){}

    public RoleAuthorizationKeyCacheEntry(Set<String> authorizationKeys){
        if (authorizationKeys != null) {
            this.authorizationKeys.addAll(authorizationKeys);
        }
    }

    public Set<String> getAuthorizationKeys() {
        return authorizationKeys;
    }

    public void setAuthorizationKeys(Set<String> authorizationKeys) {
        this.authorizationKeys = authorizationKeys;
    }
}
