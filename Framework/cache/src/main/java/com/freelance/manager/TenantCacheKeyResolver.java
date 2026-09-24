package com.freelance.manager;

import com.freelance.system.SystemHardcoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class TenantCacheKeyResolver {

    private static final Logger logger = LoggerFactory.getLogger(TenantCacheKeyResolver.class);
    public static final String DEFAULT_TENANT = SystemHardcoding.DEFAULT_TENANT.asString();

    private static final class Holder {
        private static final TenantCacheKeyResolver INSTANCE = new TenantCacheKeyResolver();
    }

    public static TenantCacheKeyResolver getInstance() {
        return Holder.INSTANCE;
    }

    private Supplier<String> tenantSupplier = () -> DEFAULT_TENANT;

    public void setTenantSupplier(Supplier<String> tenantSupplier) {
        this.tenantSupplier = tenantSupplier;
    }

    public Supplier<String> getTenantSupplier() {
        return this.tenantSupplier;
    }

    public void reset() {
        this.tenantSupplier = () -> DEFAULT_TENANT;
    }

    public <K> String resolveTenantKey(K key) {
        if (key == null) {
            return null;
        }

        String tenant = null;
        if (tenantSupplier != null) {
            try {
                tenant = tenantSupplier.get();
            } catch (Exception e) {
                logger.debug("[TenantCacheKeyResolver] Error resolving tenantId from tenantSupplier: {}", e.getMessage());
            }
        }

        String resolvedTenant = (tenant != null && !tenant.isBlank())
                ? tenant.trim().toLowerCase()
                : DEFAULT_TENANT;

        return resolvedTenant + ":" + key;
    }
}
