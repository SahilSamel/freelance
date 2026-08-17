package com.freelance;

import com.freelance.manager.CacheManager;

/**
 * Facade over the application's config-property and error-message caches.
 * Owns the DB access used to load a single value on a cache miss;
 * CacheManager owns storage, expiry, and thread-safety.
 */
public final class ConfigurationEngine {

    private ConfigurationEngine() {

    }

    /**
     * Returns the raw string value for the given key, or null if absent.
     */
    public static String get(String key) {
        return CacheManager.getInstance().get("cache_SystemConfig", key);
    }

    /**
     * Returns the string value for the given key, or the provided default if absent.
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Returns the value for the given key as an Integer, or null if absent or unparseable.
     */
    public static Integer getInt(String key) {
        String value = get(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the value for the given key as an int, or the provided default if absent or unparseable.
     */
    public static int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Returns the user-facing localized message for the given error code, or null if no mapping exists.
     */
    public static String getErrorMessage(String errorCode) {
        return CacheManager.getInstance().get("cache_ErrorMessage", errorCode);
    }

    /**
     * Forces a single config key to reload from the database on its next lookup.
     */
    public static void invalidateConfig(String key) {
        CacheManager.getInstance().invalidate("cache_SystemConfig", key);
    }

    /**
     * Forces a single error code's message to reload from the database on its next lookup.
     */
    public static void invalidateErrorMessage(String errorCode) {
        CacheManager.getInstance().invalidate("cache_ErrorMessage", errorCode);
    }
}