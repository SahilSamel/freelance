package com.freelance.config.data;

public record CacheDefinition(String name, long ttlSeconds, CacheType type) {

    public CacheDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Cache name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Cache type cannot be null");
        }
    }

    public boolean hasExpiry() {
        return ttlSeconds > 0;
    }
}