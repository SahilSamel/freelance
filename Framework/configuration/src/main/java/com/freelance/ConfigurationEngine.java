package com.freelance;

import com.freelance.cache.config.AppConfigCache;
import com.freelance.cache.config.ErrorMessageCache;
import com.freelance.manager.CacheManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

/**
 * Facade over the application's config-property and error-message caches.
 * Owns the DB access used to load a single value on a cache miss;
 * CacheManager owns storage, expiry, and thread-safety.
 */
public final class ConfigurationEngine {

    private static EntityManagerFactory entityManagerFactory;

    private ConfigurationEngine() {
        // Enforce static utility pattern
    }

    /**
     * Binds the EntityManagerFactory used to load values on a cache miss.
     * Must be called once at application boot, before any get()/
     * getErrorMessage() call.
     */
    public static void init(EntityManagerFactory emf) {
        entityManagerFactory = emf;
    }

    /**
     * Returns the raw string value for the given key, or null if absent.
     */
    public static String get(String key) {
        return CacheManager.getInstance().get(AppConfigCache.NAME, key);
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
        return CacheManager.getInstance().get(ErrorMessageCache.NAME, errorCode);
    }

    /**
     * Clears every cached config property and error message, forcing all keys to reload on next use.
     */
    public static void clear() {
        CacheManager.getInstance().clear(AppConfigCache.NAME);
        CacheManager.getInstance().clear(ErrorMessageCache.NAME);
    }

    /**
     * Forces a single config key to reload from the database on its next lookup.
     */
    public static void invalidateConfig(String key) {
        CacheManager.getInstance().invalidate(AppConfigCache.NAME, key);
    }

    /**
     * Forces a single error code's message to reload from the database on its next lookup.
     */
    public static void invalidateErrorMessage(String errorCode) {
        CacheManager.getInstance().invalidate(ErrorMessageCache.NAME, errorCode);
    }

    // --- DB access, used internally by AppConfigCache/ErrorMessageCache as their loaders ---

    public static String loadConfigValue(String configId) {
        requireInitialized();
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            List<?> rows = em.createNativeQuery(
                            "SELECT config_value FROM FL_SYSTEM_CONFIGURATION WHERE config_id = ?1")
                    .setParameter(1, configId)
                    .getResultList();
            return rows.isEmpty() ? null : (String) rows.getFirst();
        }
    }

    public static String loadErrorMessage(String errorId) {
        requireInitialized();
        try (EntityManager em = entityManagerFactory.createEntityManager()) {
            List<?> rows = em.createNativeQuery(
                            "SELECT ui_message FROM FL_ERROR_MESSAGE WHERE error_id = ?1")
                    .setParameter(1, errorId)
                    .getResultList();
            return rows.isEmpty() ? null : (String) rows.getFirst();
        }
    }

    private static void requireInitialized() {
        if (entityManagerFactory == null) {
            throw new IllegalStateException("ConfigurationEngine has not been initialized. Call init() at application boot.");
        }
    }
}