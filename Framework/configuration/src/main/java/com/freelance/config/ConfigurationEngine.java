package com.freelance.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-speed, database-backed in-memory cache for application properties
 * and error message mappings. Structurally isolated from request-thread
 * transactions to avoid deadlocks.
 */
public final class ConfigurationEngine {

    private static final ConcurrentHashMap<String, String> appConfigs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> errorMessages = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    private ConfigurationEngine() {
        // Enforce static utility pattern
    }

    /**
     * Performs the initial load from the database. Invoked exactly once
     * during application boot; subsequent calls are no-ops.
     */
    public static synchronized void initializeFromDatabase(EntityManagerFactory emf) {
        if (initialized) return;
        load(emf);
        initialized = true;
    }

    /**
     * Clears the in-memory caches and reloads them from the database.
     * Safe to call on a live server (e.g. via an administrative trigger).
     */
    public static synchronized void refresh(EntityManagerFactory emf) {
        appConfigs.clear();
        errorMessages.clear();
        load(emf);
    }

    /**
     * Loads configuration and error message data from the database into
     * the in-memory caches, using a short-lived EntityManager.
     */
    private static void load(EntityManagerFactory emf) {
        if (emf == null) {
            throw new IllegalStateException("Cannot synchronize configurations: Provided EntityManagerFactory is null!");
        }

        try (EntityManager em = emf.createEntityManager()) {

            @SuppressWarnings("unchecked")
            List<Object[]> configs = em.createNativeQuery("SELECT config_id, config_value FROM system_configuration").getResultList();
            for (Object[] row : configs) {
                if (row[0] != null && row[1] != null) {
                    appConfigs.put((String) row[0], (String) row[1]);
                }
            }

            @SuppressWarnings("unchecked")
            List<Object[]> errors = em.createNativeQuery("SELECT error_id, ui_message FROM error_message_mapping").getResultList();
            for (Object[] row : errors) {
                if (row[0] != null && row[1] != null) {
                    errorMessages.put((String) row[0], (String) row[1]);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fatal: Infrastructure synchronization failure within com.freelance.config.ConfigurationEngine loader context.", e);
        }
    }

    /**
     * Returns the raw string value for the given key, or null if absent.
     */
    public static String get(String key) {
        return appConfigs.get(key);
    }

    /**
     * Returns the string value for the given key, or the provided default
     * if absent.
     */
    public static String get(String key, String defaultValue) {
        String value = appConfigs.get(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Returns the value for the given key as an Integer, or null if the
     * key is absent or the value can't be parsed as an integer.
     */
    public static Integer getInt(String key) {
        String value = appConfigs.get(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the value for the given key as an int, or the provided
     * default if absent or unparseable.
     */
    public static int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Returns the user-facing localized message for the given error code,
     * or null if no mapping exists.
     */
    public static String getErrorMessage(String errorCode) {
        return errorMessages.get(errorCode);
    }
}