package com.freelance.cache;

import com.freelance.cache.test.fixtures.primary.FooCache;
import com.freelance.config.CacheRegistry;
import com.freelance.manager.CacheManager;
import com.freelance.manager.TenantCacheKeyResolver;
import com.freelance.provider.CacheProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheManager.
 */
class CacheManagerTest {

    private static final String PRIMARY_PKG = "com.freelance.cache.test.fixtures.primary";
    private static final String DUPE_PKG    = "com.freelance.cache.test.fixtures.dupe";

    @BeforeEach
    void resetCache() {
        CacheRegistry.getInstance().reset();
        TenantCacheKeyResolver.getInstance().reset();
        CacheProviderFactory.getInstance().reset();
    }

    // -----------------------------------------------------------------------
    // Boot behaviour
    // -----------------------------------------------------------------------

    @Test
    void beforeInit_get_throws() {
        assertThrows(IllegalStateException.class,
                () -> CacheManager.get("FooCache", "key"));
    }

    @Test
    void init_registersDiscoveredConfigs() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.get("FooCache", "any"));
    }

    @Test
    void init_parameterless_usesDefaultMandatedPackage() {
        assertEquals("com.freelance.cache.config", CacheRegistry.MANDATED_CACHE_CONFIG_PACKAGE);
        assertDoesNotThrow(() -> CacheRegistry.getInstance().init());
    }

    // -----------------------------------------------------------------------
    // get()
    // -----------------------------------------------------------------------

    @Test
    void get_byName_invokesLoader() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        String result = CacheManager.get("FooCache", "hello");
        assertEquals("foo-value-hello", result);
    }

    @Test
    void get_byClass_invokesLoader() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        String result = CacheManager.get(FooCache.class, "world");
        assertEquals("foo-value-world", result);
    }

    @Test
    void get_nullCacheName_returnsNull() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        assertNull(CacheManager.get((String) null, "key"));
    }

    @Test
    void get_nullKey_returnsNull() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        assertNull(CacheManager.get("FooCache", (String) null));
    }

    @Test
    void get_unknownCacheName_throws() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        assertThrows(IllegalStateException.class,
                () -> CacheManager.get("NonExistentCache", "key"));
    }

    // -----------------------------------------------------------------------
    // put()
    // -----------------------------------------------------------------------

    @Test
    void put_byName_overwritesEntry() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.put("FooCache", "myKey", "overridden");
        assertEquals("overridden", CacheManager.get("FooCache", "myKey"));
    }

    @Test
    void put_byClass_overwritesEntry() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.put(FooCache.class, "myKey", "class-overridden");
        assertEquals("class-overridden", CacheManager.get("FooCache", "myKey"));
    }

    @Test
    void put_nullValue_ignored() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.put("FooCache", "myKey", (String) null));
    }

    // -----------------------------------------------------------------------
    // preload()
    // -----------------------------------------------------------------------

    @Test
    void preload_byName_populatesMultipleKeys() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.preload("FooCache", Map.of("k1", "v1", "k2", "v2"));
        assertEquals("v1", CacheManager.get("FooCache", "k1"));
        assertEquals("v2", CacheManager.get("FooCache", "k2"));
    }

    @Test
    void preload_byClass_populatesMultipleKeys() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.preload(FooCache.class, Map.of("k3", "v3"));
        assertEquals("v3", CacheManager.get("FooCache", "k3"));
    }

    @Test
    void preload_nullMap_ignored() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.preload("FooCache", null));
    }

    // -----------------------------------------------------------------------
    // invalidate()
    // -----------------------------------------------------------------------

    @Test
    void invalidate_byName_removesEntry() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.put("FooCache", "myKey", "cached");
        CacheManager.invalidate("FooCache", "myKey");
        assertEquals("foo-value-myKey", CacheManager.get("FooCache", "myKey"));
    }

    @Test
    void invalidate_byClass_removesEntry() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.put(FooCache.class, "myKey", "cached");
        CacheManager.invalidate(FooCache.class, "myKey");
        assertEquals("foo-value-myKey", CacheManager.get("FooCache", "myKey"));
    }

    @Test
    void invalidate_nullKey_ignored() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.invalidate("FooCache", (String) null));
    }

    // -----------------------------------------------------------------------
    // clear()
    // -----------------------------------------------------------------------

    @Test
    void clear_byName_wipesCache() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.put("FooCache", "a", "1");
        CacheManager.put("FooCache", "b", "2");
        CacheManager.clear("FooCache");
        assertEquals("foo-value-a", CacheManager.get("FooCache", "a"));
        assertEquals("foo-value-b", CacheManager.get("FooCache", "b"));
    }

    @Test
    void clear_byClass_wipesCache() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);
        CacheManager.put(FooCache.class, "a", "1");
        CacheManager.clear(FooCache.class);
        assertEquals("foo-value-a", CacheManager.get("FooCache", "a"));
    }

    // -----------------------------------------------------------------------
    // Error cases
    // -----------------------------------------------------------------------

    @Test
    void duplicateCacheName_throwsAtInit() {
        assertThrows(IllegalStateException.class,
                () -> CacheRegistry.getInstance().init(DUPE_PKG));
    }

    @Test
    void preload_byClass_and_nullGuards() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);

        CacheManager.preload(FooCache.class, Map.of("k1", "v1", "k2", "v2"));
        assertEquals("v1", CacheManager.get(FooCache.class, "k1"));

        assertDoesNotThrow(() -> CacheManager.preload((String) null, Map.of("a", "b")));
        assertDoesNotThrow(() -> CacheManager.preload("FooCache", null));
        assertDoesNotThrow(() -> CacheManager.preload("FooCache", Map.of()));
    }

    @Test
    void nullAndInvalidParameterGuards() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);

        assertDoesNotThrow(() -> CacheManager.invalidate((String) null, "key"));
        assertDoesNotThrow(() -> CacheManager.clear((String) null));

        assertThrows(IllegalStateException.class, () -> CacheManager.get("NonExistent", "key"));

        class UnregisteredConfig extends com.freelance.config.AbstractCacheConfig<String, String> {
            @Override public String name() { return "Unregistered"; }
            @Override public com.freelance.config.data.CacheType type() { return com.freelance.config.data.CacheType.IN_MEMORY; }
            @Override public long ttlSeconds() { return 60; }
            @Override public String load(String key) { return null; }
        }
        assertThrows(IllegalStateException.class, () -> CacheManager.get(UnregisteredConfig.class, "key"));
    }

    @Test
    void privateConstructor_reflection() throws Exception {
        java.lang.reflect.Constructor<CacheManager> ctor = CacheManager.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        CacheManager instance = ctor.newInstance();
        assertNotNull(instance);
    }

    // -----------------------------------------------------------------------
    // Multi-tenant key isolation tests
    // -----------------------------------------------------------------------

    @Test
    void tenantIsolation_getAndPut_separateValuesAcrossTenants() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);

        final String[] activeTenant = {"tenant_alpha"};
        TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> activeTenant[0]);

        CacheManager.put("FooCache", "shared_key", "value_for_alpha");
        assertEquals("value_for_alpha", CacheManager.get("FooCache", "shared_key"));

        activeTenant[0] = "tenant_beta";
        assertEquals("foo-value-shared_key", CacheManager.get("FooCache", "shared_key"));

        CacheManager.put("FooCache", "shared_key", "value_for_beta");
        assertEquals("value_for_beta", CacheManager.get("FooCache", "shared_key"));

        activeTenant[0] = "tenant_alpha";
        assertEquals("value_for_alpha", CacheManager.get("FooCache", "shared_key"));
    }

    @Test
    void tenantIsolation_preload_isolatesByTenant() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);

        final String[] activeTenant = {"tenant_one"};
        TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> activeTenant[0]);

        CacheManager.preload("FooCache", Map.of("k1", "v1_one", "k2", "v2_one"));
        assertEquals("v1_one", CacheManager.get("FooCache", "k1"));

        activeTenant[0] = "tenant_two";
        CacheManager.preload("FooCache", Map.of("k1", "v1_two"));
        assertEquals("v1_two", CacheManager.get("FooCache", "k1"));

        activeTenant[0] = "tenant_one";
        assertEquals("v1_one", CacheManager.get("FooCache", "k1"));
    }

    @Test
    void tenantIsolation_invalidate_onlyRemovesTargetTenantEntry() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);

        final String[] activeTenant = {"tenant_x"};
        TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> activeTenant[0]);

        CacheManager.put("FooCache", "k1", "custom_x");

        activeTenant[0] = "tenant_y";
        CacheManager.put("FooCache", "k1", "custom_y");

        activeTenant[0] = "tenant_x";
        CacheManager.invalidate("FooCache", "k1");
        assertEquals("foo-value-k1", CacheManager.get("FooCache", "k1"));

        activeTenant[0] = "tenant_y";
        assertEquals("custom_y", CacheManager.get("FooCache", "k1"));
    }

    @Test
    void tenantFallback_whenSupplierNullBlankOrThrows() {
        CacheRegistry.getInstance().init(PRIMARY_PKG);

        TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> null);
        CacheManager.put("FooCache", "k", "default_val");
        assertEquals("default_val", CacheManager.get("FooCache", "k"));

        TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> "   ");
        assertEquals("default_val", CacheManager.get("FooCache", "k"));

        TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> {
            throw new RuntimeException("Context unavailable");
        });
        assertEquals("default_val", CacheManager.get("FooCache", "k"));
        assertNotNull(TenantCacheKeyResolver.getInstance().getTenantSupplier());
    }
}
