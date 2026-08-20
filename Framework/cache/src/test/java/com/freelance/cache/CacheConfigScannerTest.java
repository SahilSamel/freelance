package com.freelance.cache;

import com.freelance.cache.test.fixtures.primary.BarCache;
import com.freelance.cache.test.fixtures.primary.FooCache;
import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.scanner.CacheConfigScanner;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigScannerTest {

    private final CacheConfigScanner scanner = new CacheConfigScanner();

    @Test
    void scan_findsConcreteSubclassesInPackage() {
        Set<Class<? extends AbstractCacheConfig<?, ?>>> found =
                scanner.scan("com.freelance.cache.test.fixtures.primary");

        assertEquals(2, found.size(), "Should find exactly FooCache and BarCache");
        assertTrue(found.contains(FooCache.class));
        assertTrue(found.contains(BarCache.class));
    }

    @Test
    void scan_nonExistentPackage_returnsEmptySet() {
        Set<Class<? extends AbstractCacheConfig<?, ?>>> found =
                scanner.scan("com.freelance.cache.test.nonexistent");

        assertNotNull(found);
        assertTrue(found.isEmpty());
    }
}
