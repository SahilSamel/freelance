package com.freelance.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantCacheKeyResolver Unit Tests")
class TenantCacheKeyResolverTest {

    private TenantCacheKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TenantCacheKeyResolver();
    }

    @Test
    @DisplayName("resolveTenantKey returns null when key is null")
    void resolveTenantKey_nullKey_returnsNull() {
        assertNull(resolver.resolveTenantKey(null));
    }

    @Test
    @DisplayName("resolveTenantKey uses default tenant when supplier returns null or blank")
    void resolveTenantKey_defaultTenant_whenSupplierBlank() {
        assertEquals("freelance:my-key", resolver.resolveTenantKey("my-key"));

        resolver.setTenantSupplier(() -> null);
        assertEquals("freelance:my-key", resolver.resolveTenantKey("my-key"));

        resolver.setTenantSupplier(() -> "   ");
        assertEquals("freelance:my-key", resolver.resolveTenantKey("my-key"));
    }

    @Test
    @DisplayName("resolveTenantKey formats active tenant properly and handles case normalization")
    void resolveTenantKey_activeTenant() {
        resolver.setTenantSupplier(() -> "IronFit_Gym");
        assertEquals("ironfit_gym:user-100", resolver.resolveTenantKey("user-100"));
    }

    @Test
    @DisplayName("resolveTenantKey handles exceptions from supplier gracefully")
    void resolveTenantKey_supplierException_fallsBackToDefault() {
        resolver.setTenantSupplier(() -> {
            throw new IllegalStateException("Thread context lost");
        });
        assertEquals("freelance:fallback-key", resolver.resolveTenantKey("fallback-key"));
    }

    @Test
    @DisplayName("reset reverts tenantSupplier to default tenant")
    void reset_revertsToDefault() {
        resolver.setTenantSupplier(() -> "custom_tenant");
        assertEquals("custom_tenant:key", resolver.resolveTenantKey("key"));

        resolver.reset();
        assertEquals("freelance:key", resolver.resolveTenantKey("key"));
        assertNotNull(resolver.getTenantSupplier());
    }
}
