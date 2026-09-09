package com.freelance.mapping.roleAuthorization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RoleAuthorizationKey implements Serializable {

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "authorization_id")
    private UUID authorizationId;

    public RoleAuthorizationKey() {}

    public RoleAuthorizationKey(UUID roleId, UUID authorizationId) {
        this.roleId = roleId;
        this.authorizationId = authorizationId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(UUID authorizationId) {
        this.authorizationId = authorizationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleAuthorizationKey that = (RoleAuthorizationKey) o;
        return Objects.equals(roleId, that.roleId) &&
                Objects.equals(authorizationId, that.authorizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, authorizationId);
    }
}