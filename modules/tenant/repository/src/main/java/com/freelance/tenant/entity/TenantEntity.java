package com.freelance.tenant.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "FL_TENANTS")
@NamedQueries({
        @NamedQuery(
                name = TenantEntity.QUERY_FIND_ALL,
                query = "SELECT t FROM TenantEntity t ORDER BY t.createdAt ASC"
        ),
        @NamedQuery(
                name = TenantEntity.QUERY_FIND_ALL_ACTIVE,
                query = "SELECT t FROM TenantEntity t WHERE t.status = 'ACTIVE' ORDER BY t.createdAt ASC"
        ),
        @NamedQuery(
                name = TenantEntity.QUERY_FIND_BY_TENANT_ID,
                query = "SELECT t FROM TenantEntity t WHERE t.tenantId = :tenantId"
        )
})
public class TenantEntity {

    public static final String QUERY_FIND_ALL = "TenantEntity.findAll";
    public static final String QUERY_FIND_ALL_ACTIVE = "TenantEntity.findAllActive";
    public static final String QUERY_FIND_BY_TENANT_ID = "TenantEntity.findByTenantId";

    @Id
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TenantEntity() {
    }

    public TenantEntity(String tenantId, String name, String status) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
