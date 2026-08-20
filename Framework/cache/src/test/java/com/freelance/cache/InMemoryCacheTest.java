package com.freelance.cache;

import com.freelance.config.data.CacheDefinition;
import com.freelance.config.data.CacheType;
import com.freelance.provider.impl.InMemoryCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCacheTest {

    // Shared definitions
    private static final CacheDefinition NO_TTL =
            new CacheDefinition("test-no-ttl", -1, CacheType.IN_MEMORY);
    private static final CacheDefinition ONE_SEC_TTL =
            new CacheDefinition("test-ttl", 1, CacheType.IN_MEMORY);

    private InMemoryCache cache; // fresh no-TTL instance per test

    @BeforeEach
    void setUp() {
        cache = new InMemoryCache(NO_TTL);
    }

    // -----------------------------------------------------------------------
    // get()
    // -----------------------------------------------------------------------

    @Test
    void get_miss_invokesLoader() {
        Object result = cache.get("k1", () -> "value1");
        assertEquals("value1", result);
    }

    @Test
    void get_hit_doesNotInvokeLoader() {
        cache.put("k1", "value1");
        AtomicInteger calls = new AtomicInteger();
        Object result = cache.get("k1", () -> { calls.incrementAndGet(); return "other"; });
        assertEquals("value1", result);
        assertEquals(0, calls.get(), "Loader must not be called on a cache hit");
    }

    @Test
    void get_expired_reloads() throws InterruptedException {
        InMemoryCache ttlCache = new InMemoryCache(ONE_SEC_TTL);
        ttlCache.put("k1", "original");
        Thread.sleep(1100); // outlive the 1-second TTL
        Object result = ttlCache.get("k1", () -> "reloaded");
        assertEquals("reloaded", result);
    }

    @Test
    void get_nullKey_returnsNull() {
        assertNull(cache.get(null, () -> "value"));
    }

    @Test
    void get_nullLoader_returnsNull() {
        assertNull(cache.get("missing", null));
    }

    @Test
    void get_loaderReturnsNull_notCached() {
        // A null result from load() must never be stored — the next get() must call the loader again.
        AtomicInteger calls = new AtomicInteger();
        cache.get("k1", () -> { calls.incrementAndGet(); return null; });
        cache.get("k1", () -> { calls.incrementAndGet(); return null; });
        assertEquals(2, calls.get(), "Loader must be called again because null was not cached");
    }

    // -----------------------------------------------------------------------
    // put()
    // -----------------------------------------------------------------------

    @Test
    void put_storesValue() {
        cache.put("k1", "stored");
        assertEquals("stored", cache.get("k1", () -> "other"));
    }

    @Test
    void put_nullKey_ignored() {
        assertDoesNotThrow(() -> cache.put(null, "value"));
    }

    @Test
    void put_nullValue_ignored() {
        assertDoesNotThrow(() -> cache.put("k1", null));
        // Nothing stored — loader must still be invoked
        AtomicInteger calls = new AtomicInteger();
        cache.get("k1", () -> { calls.incrementAndGet(); return "fallback"; });
        assertEquals(1, calls.get());
    }

    @Test
    void put_overwritesExistingEntry() {
        cache.put("k1", "first");
        cache.put("k1", "second");
        assertEquals("second", cache.get("k1", () -> "other"));
    }

    // -----------------------------------------------------------------------
    // invalidate()
    // -----------------------------------------------------------------------

    @Test
    void invalidate_removesEntry() {
        cache.put("k1", "value1");
        cache.invalidate("k1");
        AtomicInteger calls = new AtomicInteger();
        cache.get("k1", () -> { calls.incrementAndGet(); return "reloaded"; });
        assertEquals(1, calls.get(), "Loader must be invoked after invalidation");
    }

    @Test
    void invalidate_nullKey_noException() {
        assertDoesNotThrow(() -> cache.invalidate(null));
    }

    // -----------------------------------------------------------------------
    // clear()
    // -----------------------------------------------------------------------

    @Test
    void clear_removesAllEntries() {
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.clear();
        AtomicInteger calls = new AtomicInteger();
        cache.get("a", () -> { calls.incrementAndGet(); return "x"; });
        cache.get("b", () -> { calls.incrementAndGet(); return "x"; });
        cache.get("c", () -> { calls.incrementAndGet(); return "x"; });
        assertEquals(3, calls.get(), "All keys must be misses after clear()");
    }

    // -----------------------------------------------------------------------
    // TTL edge cases
    // -----------------------------------------------------------------------

    @Test
    void noTtl_entryNeverExpires() throws InterruptedException {
        cache.put("k1", "persistent");
        Thread.sleep(1100); // same wait as the TTL test — must NOT expire
        assertEquals("persistent", cache.get("k1", () -> "other"));
    }
}
