package com.freelance.entity.mapping.roleAuthorization;

import com.freelance.entity.authorization.Authorization;
import com.freelance.entity.role.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@NamedQueries({
        @NamedQuery(
                name = RoleAuthorization.QUERY_FIND_ALL_WITH_DETAILS,
                query = "SELECT ra FROM RoleAuthorization ra JOIN FETCH ra.authorization JOIN FETCH ra.role"
        ),
        @NamedQuery(
                name = RoleAuthorization.QUERY_FIND_BY_ROLE_ID,
                query = "SELECT ra FROM RoleAuthorization ra WHERE ra.id.roleId = :roleId"
        ),
        @NamedQuery(
                name = RoleAuthorization.QUERY_FIND_ALL_SYSTEM_ROLE_MAPPINGS,
                query = "SELECT ra FROM RoleAuthorization ra JOIN FETCH ra.authorization JOIN FETCH ra.role WHERE ra.role.isSystem = TRUE"
        ),
        @NamedQuery(
                name = RoleAuthorization.QUERY_DELETE_BY_SYSTEM_ROLES,
                query = "DELETE FROM RoleAuthorization ra WHERE ra.role.id IN (SELECT r.id FROM Role r WHERE r.isSystem = TRUE)"
        )
})
@Table(name = "FL_ROLE_AUTHORIZATION", schema = "freelance")
public class RoleAuthorization {

    public static final String QUERY_FIND_ALL_WITH_DETAILS = "RoleAuthorization.findAllWithDetails";
    public static final String QUERY_FIND_BY_ROLE_ID = "RoleAuthorization.findByRoleId";
    public static final String QUERY_FIND_ALL_SYSTEM_ROLE_MAPPINGS = "RoleAuthorization.findAllSystemRoleMappings";
    public static final String QUERY_DELETE_BY_SYSTEM_ROLES = "RoleAuthorization.deleteBySystemRoles";

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