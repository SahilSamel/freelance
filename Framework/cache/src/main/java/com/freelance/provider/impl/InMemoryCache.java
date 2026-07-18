package com.freelance.provider.impl;

import com.freelance.config.data.CacheDefinition;
import com.freelance.provider.Cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * In-memory cache backend. Values live only in this JVM's heap and are
 * lost on restart. Expiry is checked lazily on read.
 */
public final class InMemoryCache implements Cache {

    private record Entry(Object value, long expiresAtMillis) {
        boolean isExpired() {
            return expiresAtMillis > 0 && System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private final CacheDefinition definition;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public InMemoryCache(CacheDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Object get(String key, Supplier<Object> loader) {
        Entry entry = store.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }

        // Miss or expired: compute and store. Two threads racing here both call the
        // loader and last-write-wins — acceptable for a cache, avoids per-key locking.
        Object value = loader.get();
        put(key, value);
        return value;
    }

    @Override
    public void put(String key, Object value) {
        long expiresAt = definition.hasExpiry()
                ? System.currentTimeMillis() + (definition.ttlSeconds() * 1000L)
                : -1;
        store.put(key, new Entry(value, expiresAt));
    }

    @Override
    public void invalidate(String key) {
        store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }
}
