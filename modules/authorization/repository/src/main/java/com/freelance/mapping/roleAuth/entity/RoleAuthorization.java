package com.freelance.mapping.roleAuth.entity;

import com.freelance.authorization.entity.Authorization;
import com.freelance.role.entity.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "FL_ROLE_AUTHORIZATION", schema = "freelance")
public class RoleAuthorization {

    @EmbeddedId
    private RoleAuthorizationKey id = new RoleAuthorizationKey();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("authorizationId")
    @JoinColumn(name = "authorization_id", nullable = false)
    private Authorization authorization;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    public RoleAuthorization() {}

    public RoleAuthorization(Role role, Authorization authorization) {
        this.role = role;
        this.authorization = authorization;
        this.id = new RoleAuthorizationKey(
                role != null ? role.getId() : null,
                authorization != null ? authorization.getId() : null
        );
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
    }

    public RoleAuthorizationKey getId() {
        return id;
    }

    public void setId(RoleAuthorizationKey id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
        if (role != null) {
            this.id.setRoleId(role.getId());
        }
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
        if (authorization != null) {
            this.id.setAuthorizationId(authorization.getId());
        }
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}