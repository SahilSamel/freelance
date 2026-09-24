package com.freelance.manager;

import com.freelance.cache.test.fixtures.primary.FooCache;
import com.freelance.provider.Cache;
import com.freelance.provider.CacheProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CacheProviderFactory Unit Tests")
class CacheProviderFactoryTest {

    private CacheProviderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CacheProviderFactory();
    }

    @Test
    @DisplayName("get returns null before cache is created")
    void get_uncreatedCache_returnsNull() {
        assertNull(factory.get("FooCache"));
    }

    @Test
    @DisplayName("getOrCreate instantiates and caches provider instance")
    void getOrCreate_createsAndCaches() {
        FooCache config = new FooCache();
        Cache cache1 = factory.getOrCreate("FooCache", config);
        assertNotNull(cache1);

        Cache cache2 = factory.getOrCreate("FooCache", config);
        assertSame(cache1, cache2);

        assertSame(cache1, factory.get("FooCache"));
    }
}
