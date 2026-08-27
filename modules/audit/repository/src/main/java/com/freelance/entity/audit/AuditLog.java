package com.freelance.entity.audit;

import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "FL_AUDIT_LOG", schema = "freelance")
public class AuditLog extends AbstractAuditEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "trace_id", nullable = false, updatable = false, length = 255)
    private String traceId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "tenant_id", updatable = false, length = 255)
    private String tenantId;

    @Column(name = "http_method", nullable = false, updatable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint", nullable = false, updatable = false, length = 1024)
    private String endpoint;

    @Column(name = "status_code", nullable = false, updatable = false)
    private int statusCode;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private long durationMs;

    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "auth_mechanism", updatable = false, length = 50)
    private String authMechanism;

    public AuditLog() {
    }

    public AuditLog(
            Instant timestamp,
            String traceId,
            UUID userId,
            String tenantId,
            String httpMethod,
            String endpoint,
            int statusCode,
            long durationMs,
            String ipAddress,
            String userAgent,
            String authMechanism
    ) {
        this.timestamp = timestamp;
        this.traceId = traceId;
        this.userId = userId;
        this.tenantId = tenantId;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.authMechanism = authMechanism;
    }

    /**
     * Assigns primary key UUID and timestamp if not already provided prior to insertion.
     */
    @PrePersist
    protected void onPersistAudit() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }
}
