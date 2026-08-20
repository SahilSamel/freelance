package com.freelance.provider.impl;

import com.freelance.config.data.CacheDefinition;
import com.freelance.provider.Cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * In-memory cache backend. Values live only in this JVM's heap and are
 * lost on restart. Expiry is checked lazily on read, with periodic background sweeps.
 */
public final class InMemoryCache implements Cache {

    private static final ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "in-memory-cache-cleaner");
                thread.setDaemon(true);
                return thread;
            });

    private record Entry(Object value, long expiresAtMillis) {
        boolean isExpired() {
            return expiresAtMillis > 0 && System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private final CacheDefinition definition;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public InMemoryCache(CacheDefinition definition) {
        this.definition = definition;
        if (definition.hasExpiry()) {
            CLEANER.scheduleAtFixedRate(this::evictExpired, 5, 5, TimeUnit.MINUTES);
        }
    }

    private void evictExpired() {
        store.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @Override
    public Object get(String key, Supplier<Object> loader) {
        if (key == null) {
            return null;
        }

        Entry entry = store.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value();
        }

        if (loader == null) {
            return null;
        }

        // Miss or expired: compute and store non-null values.
        Object value = loader.get();
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    @Override
    public void put(String key, Object value) {
        if (key == null || value == null) {
            return;
        }

        long expiresAt = definition.hasExpiry()
                ? System.currentTimeMillis() + (definition.ttlSeconds() * 1000L)
                : -1;
        store.put(key, new Entry(value, expiresAt));
    }

    @Override
    public void invalidate(String key) {
        if (key != null) {
            store.remove(key);
        }
    }

    @Override
    public void clear() {
        store.clear();
    }
}

