package com.freelance.rest.jaxrs.tenant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory cache of tenant statuses for ultra-fast validation in request filters.
 */
public final class TenantRegistry {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DEACTIVATED = "DEACTIVATED";

    private static final Map<String, String> TENANT_STATUS_MAP = new ConcurrentHashMap<>();

    private TenantRegistry() {
    }

    public static void registerTenant(String tenantId, String status) {
        if (tenantId != null && !tenantId.isBlank()) {
            TENANT_STATUS_MAP.put(tenantId.trim().toLowerCase(), status != null ? status.trim().toUpperCase() : STATUS_ACTIVE);
        }
    }

    public static void setStatus(String tenantId, String status) {
        registerTenant(tenantId, status);
    }

    public static boolean isRegistered(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return TENANT_STATUS_MAP.containsKey(tenantId.trim().toLowerCase());
    }

    public static boolean isActive(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        String status = TENANT_STATUS_MAP.get(tenantId.trim().toLowerCase());
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public static boolean hasAnyTenantsRegistered() {
        return !TENANT_STATUS_MAP.isEmpty();
    }

    public static void clear() {
        TENANT_STATUS_MAP.clear();
    }
}
