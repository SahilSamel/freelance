package com.freelance.cache.config;

import java.io.Serializable;
import java.time.Instant;

/**
 * Cache entry holding runtime support access state including granted expiration timestamp
 * and pending access request metadata.
 */
public class SupportAccessCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Instant expiresAt;
    private String pendingReason;
    private String pendingTicket;

    public SupportAccessCacheEntry() {
    }

    public SupportAccessCacheEntry(Instant expiresAt, String pendingReason, String pendingTicket) {
        this.expiresAt = expiresAt;
        this.pendingReason = pendingReason;
        this.pendingTicket = pendingTicket;
    }

    public boolean isActive() {
        return expiresAt != null && Instant.now().isBefore(expiresAt);
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getPendingReason() {
        return pendingReason;
    }

    public void setPendingReason(String pendingReason) {
        this.pendingReason = pendingReason;
    }

    public String getPendingTicket() {
        return pendingTicket;
    }

    public void setPendingTicket(String pendingTicket) {
        this.pendingTicket = pendingTicket;
    }

    public void clear() {
        this.expiresAt = null;
        this.pendingReason = null;
        this.pendingTicket = null;
    }
}
