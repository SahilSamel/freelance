package com.freelance.cache;

import com.freelance.config.data.CacheDefinition;
import com.freelance.config.data.CacheType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheDefinitionTest {

    @Test
    void nullName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new CacheDefinition(null, -1, CacheType.IN_MEMORY));
    }

    @Test
    void blankName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new CacheDefinition("   ", -1, CacheType.IN_MEMORY));
    }

    @Test
    void nullType_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new CacheDefinition("myCache", -1, null));
    }

    @Test
    void hasExpiry_trueWhenPositiveTtl() {
        CacheDefinition def = new CacheDefinition("myCache", 60, CacheType.IN_MEMORY);
        assertTrue(def.hasExpiry());
    }

    @Test
    void hasExpiry_falseWhenMinusOne() {
        CacheDefinition def = new CacheDefinition("myCache", -1, CacheType.IN_MEMORY);
        assertFalse(def.hasExpiry());
    }

    @Test
    void hasExpiry_falseWhenZero() {
        CacheDefinition def = new CacheDefinition("myCache", 0, CacheType.IN_MEMORY);
        assertFalse(def.hasExpiry());
    }
}
