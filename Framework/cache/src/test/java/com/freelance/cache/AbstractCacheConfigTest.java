package com.freelance.cache;

import com.freelance.config.AbstractCacheConfig;
import com.freelance.config.data.CacheDefinition;
import com.freelance.config.data.CacheType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractCacheConfigTest {

    // --- Fixtures ---

    /** Uses all defaults: name = class name, ttl = -1, type = IN_MEMORY. */
    static class DefaultConfig extends AbstractCacheConfig<String, String> {
        @Override
        protected String load(String key) {
            return "loaded-" + key;
        }
    }

    /** Overrides every configurable field. */
    static class OverriddenConfig extends AbstractCacheConfig<String, String> {
        @Override
        protected String name() { return "MyCustomCache"; }

        @Override
        protected long ttlSeconds() { return 120L; }

        @Override
        protected CacheType type() { return CacheType.IN_MEMORY; }

        @Override
        protected String load(String key) { return "custom-" + key; }
    }

    // --- Tests ---

    @Test
    void defaultName_isClassName() {
        assertEquals("DefaultConfig", new DefaultConfig().toDefinition().name());
    }

    @Test
    void defaultTtl_isMinusOne() {
        assertEquals(-1L, new DefaultConfig().toDefinition().ttlSeconds());
    }

    @Test
    void defaultType_isInMemory() {
        assertEquals(CacheType.IN_MEMORY, new DefaultConfig().toDefinition().type());
    }

    @Test
    void toDefinition_reflectsOverriddenValues() {
        CacheDefinition def = new OverriddenConfig().toDefinition();
        assertEquals("MyCustomCache", def.name());
        assertEquals(120L, def.ttlSeconds());
        assertEquals(CacheType.IN_MEMORY, def.type());
    }

    @Test
    void loadInternal_delegatesToLoad() {
        Object result = new DefaultConfig().loadInternal("hello");
        assertEquals("loaded-hello", result);
    }
}
