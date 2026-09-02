package com.freelance.cache;

import com.freelance.cache.test.fixtures.primary.FooCache;
import com.freelance.manager.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheManager.
 *
 * <p>CacheManager is a singleton, so we reset its internal state via reflection
 * before every test to keep each test fully self-contained.
 *
 * <p>All tests that need a registered cache call {@code init(PRIMARY_PKG)}, which
 * scans only the two tiny fixture classes (FooCache, BarCache) from the test classpath.
 * The duplicate-name test uses a separate package whose two fixtures share the same name.
 */
class CacheManagerTest {

    private static final String PRIMARY_PKG = "com.freelance.cache.test.fixtures.primary";
    private static final String DUPE_PKG    = "com.freelance.cache.test.fixtures.dupe";

    // -----------------------------------------------------------------------
    // Reset singleton before every test
    // -----------------------------------------------------------------------

    @BeforeEach
    void resetCacheManager() throws Exception {
        CacheManager mgr = CacheManager.getInstance();

        setField(mgr, "initialized", false);
        clearMap(mgr, "configsByName");
        clearMap(mgr, "nameByClass");
        clearMap(mgr, "caches");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void clearMap(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        ((Map<?, ?>) f.get(target)).clear();
    }

    // -----------------------------------------------------------------------
    // Boot behaviour
    // -----------------------------------------------------------------------

    @Test
    void beforeInit_get_throws() {
        // Manager is reset (uninitialized) — any operation must throw immediately.
        assertThrows(IllegalStateException.class,
                () -> CacheManager.getInstance().get("FooCache", "key"));
    }

    @Test
    void init_registersDiscoveredConfigs() {
        // After init, get() must succeed for a registered cache.
        CacheManager.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.getInstance().get("FooCache", "any"));
    }

    @Test
    void init_parameterless_usesDefaultMandatedPackage() {
        assertEquals("com.freelance.cache.config", CacheManager.MANDATED_CACHE_CONFIG_PACKAGE);
        assertDoesNotThrow(() -> CacheManager.getInstance().init());
    }

    // -----------------------------------------------------------------------
    // get()
    // -----------------------------------------------------------------------

    @Test
    void get_byName_invokesLoader() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        String result = CacheManager.getInstance().get("FooCache", "hello");
        assertEquals("foo-value-hello", result);
    }

    @Test
    void get_byClass_invokesLoader() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        String result = CacheManager.getInstance().get(FooCache.class, "world");
        assertEquals("foo-value-world", result);
    }

    @Test
    void get_nullCacheName_returnsNull() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        assertNull(CacheManager.getInstance().get((String) null, "key"));
    }

    @Test
    void get_nullKey_returnsNull() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        assertNull(CacheManager.getInstance().get("FooCache", (String) null));
    }

    @Test
    void get_unknownCacheName_throws() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        assertThrows(IllegalStateException.class,
                () -> CacheManager.getInstance().get("NonExistentCache", "key"));
    }

    // -----------------------------------------------------------------------
    // put()
    // -----------------------------------------------------------------------

    @Test
    void put_byName_overwritesEntry() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().put("FooCache", "myKey", "overridden");
        assertEquals("overridden", CacheManager.getInstance().get("FooCache", "myKey"));
    }

    @Test
    void put_byClass_overwritesEntry() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().put(FooCache.class, "myKey", "class-overridden");
        assertEquals("class-overridden", CacheManager.getInstance().get("FooCache", "myKey"));
    }

    @Test
    void put_nullValue_ignored() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.getInstance().put("FooCache", "myKey", (String) null));
    }

    // -----------------------------------------------------------------------
    // preload()
    // -----------------------------------------------------------------------

    @Test
    void preload_byName_populatesMultipleKeys() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().preload("FooCache", Map.of("k1", "v1", "k2", "v2"));
        assertEquals("v1", CacheManager.getInstance().get("FooCache", "k1"));
        assertEquals("v2", CacheManager.getInstance().get("FooCache", "k2"));
    }

    @Test
    void preload_byClass_populatesMultipleKeys() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().preload(FooCache.class, Map.of("k3", "v3"));
        assertEquals("v3", CacheManager.getInstance().get("FooCache", "k3"));
    }

    @Test
    void preload_nullMap_ignored() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.getInstance().preload("FooCache", null));
    }

    // -----------------------------------------------------------------------
    // invalidate()
    // -----------------------------------------------------------------------

    @Test
    void invalidate_byName_removesEntry() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().put("FooCache", "myKey", "cached");
        CacheManager.getInstance().invalidate("FooCache", "myKey");
        // After invalidation load() must fire again
        assertEquals("foo-value-myKey", CacheManager.getInstance().get("FooCache", "myKey"));
    }

    @Test
    void invalidate_byClass_removesEntry() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().put(FooCache.class, "myKey", "cached");
        CacheManager.getInstance().invalidate(FooCache.class, "myKey");
        assertEquals("foo-value-myKey", CacheManager.getInstance().get("FooCache", "myKey"));
    }

    @Test
    void invalidate_nullKey_ignored() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        assertDoesNotThrow(() -> CacheManager.getInstance().invalidate("FooCache", (String) null));
    }

    // -----------------------------------------------------------------------
    // clear()
    // -----------------------------------------------------------------------

    @Test
    void clear_byName_wipesCache() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().put("FooCache", "a", "1");
        CacheManager.getInstance().put("FooCache", "b", "2");
        CacheManager.getInstance().clear("FooCache");
        // Both keys must reload from load()
        assertEquals("foo-value-a", CacheManager.getInstance().get("FooCache", "a"));
        assertEquals("foo-value-b", CacheManager.getInstance().get("FooCache", "b"));
    }

    @Test
    void clear_byClass_wipesCache() {
        CacheManager.getInstance().init(PRIMARY_PKG);
        CacheManager.getInstance().put(FooCache.class, "a", "1");
        CacheManager.getInstance().clear(FooCache.class);
        assertEquals("foo-value-a", CacheManager.getInstance().get("FooCache", "a"));
    }

    // -----------------------------------------------------------------------
    // Error cases
    // -----------------------------------------------------------------------

    @Test
    void duplicateCacheName_throwsAtInit() {
        // The dupe package has two configs both returning "DuplicateCache" as their name.
        assertThrows(IllegalStateException.class,
                () -> CacheManager.getInstance().init(DUPE_PKG));
    }
}
