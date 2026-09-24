package com.freelance.manager;

import com.freelance.cache.test.fixtures.primary.FooCache;
import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.CacheRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CacheRegistry Unit Tests")
class CacheRegistryTest {

    private static final String PRIMARY_PKG = "com.freelance.cache.test.fixtures.primary";
    private static final String DUPE_PKG    = "com.freelance.cache.test.fixtures.dupe";

    private CacheRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CacheRegistry();
    }

    @Test
    @DisplayName("resolveConfig throws before init")
    void resolveConfig_beforeInit_throws() {
        assertThrows(IllegalStateException.class, () -> registry.resolveConfig("FooCache"));
    }

    @Test
    @DisplayName("init registers discovered classes and enables lookup")
    void init_registersAndResolves() {
        assertFalse(registry.isInitialized());
        registry.init(PRIMARY_PKG);
        assertTrue(registry.isInitialized());

        AbstractCacheConfig<?, ?> config = registry.resolveConfig("FooCache");
        assertNotNull(config);
        assertEquals("FooCache", config.toDefinition().name());

        String name = registry.resolveName(FooCache.class);
        assertEquals("FooCache", name);
    }

    @Test
    @DisplayName("resolveName returns null when configClass is null")
    void resolveName_nullClass_returnsNull() {
        registry.init(PRIMARY_PKG);
        assertNull(registry.resolveName(null));
    }

    @Test
    @DisplayName("init throws on duplicate cache names")
    void init_duplicateNames_throws() {
        assertThrows(IllegalStateException.class, () -> registry.init(DUPE_PKG));
    }

    @Test
    @DisplayName("resolveConfig throws when cache name is unregistered")
    void resolveConfig_unregisteredName_throws() {
        registry.init(PRIMARY_PKG);
        assertThrows(IllegalStateException.class, () -> registry.resolveConfig("UnknownCache"));
    }
}
