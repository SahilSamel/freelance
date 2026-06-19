package config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CORE CONFIGURATION UTILITY
 * High-speed, database-backed memory cache for application properties and error mappings.
 * Structurally isolated from request thread transactions to protect against system deadlock loops.
 */
public final class ConfigurationEngine {

    private static final ConcurrentHashMap<String, String> appConfigs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> errorMessages = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    private ConfigurationEngine() {
        // Enforce static utility pattern
    }

    /**
     * 🚀 INITIAL COLD START
     * Invoked exactly once within the Main/Launcher wrapper script during system boot.
     */
    public static synchronized void initializeFromDatabase(EntityManagerFactory emf) {
        if (initialized) return;
        load(emf);
        initialized = true;
    }

    /**
     * 🔄 HOT REBUILD
     * Wipes active RAM maps and forces a fresh query synchronization.
     * Can be safely executed on a live server via protected administrative triggers.
     */
    public static synchronized void refresh(EntityManagerFactory emf) {
        appConfigs.clear();
        errorMessages.clear();
        load(emf);
    }

    /**
     * Internal data harvester utilizing a distinct, short-lived session boundary.
     */
    private static void load(EntityManagerFactory emf) {
        if (emf == null) {
            throw new IllegalStateException("Cannot synchronize configurations: Provided EntityManagerFactory is null!");
        }

        try (EntityManager em = emf.createEntityManager()) {

            // 🎯 Updated to use 'config_id' and 'config_value'
            @SuppressWarnings("unchecked")
            List<Object[]> configs = em.createNativeQuery("SELECT config_id, config_value FROM system_configuration").getResultList();
            for (Object[] row : configs) {
                if (row[0] != null && row[1] != null) {
                    appConfigs.put((String) row[0], (String) row[1]);
                }
            }

            // 🎯 Updated to use 'error_id' and 'ui_message'
            @SuppressWarnings("unchecked")
            List<Object[]> errors = em.createNativeQuery("SELECT error_id, ui_message FROM error_message_mapping").getResultList();
            for (Object[] row : errors) {
                if (row[0] != null && row[1] != null) {
                    errorMessages.put((String) row[0], (String) row[1]);
                }
            }

            System.out.println("config.ConfigurationEngine: Local RAM caches synchronized successfully with the database.");

        } catch (Exception e) {
            throw new RuntimeException("Fatal: Infrastructure synchronization failure within config.ConfigurationEngine loader context.", e);
        }
    }

    /**
     * RAW STRING GETTER
     * Pulls value from cache. Returns null if key is non-existent or empty.
     */
    public static String get(String key) {
        return appConfigs.get(key);
    }

    /**
     * FALLBACK STRING GETTER
     * Returns matching configuration value, or falls back to default if absent.
     */
    public static String get(String key, String defaultValue) {
        String value = appConfigs.get(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * RAW INTEGER GETTER
     * Converts value to Integer. Returns null if key is absent or corrupted.
     */
    public static Integer getInt(String key) {
        String value = appConfigs.get(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Configuration format error: Key '" + key + "' cannot be cast to an Integer. Value: '" + value + "'");
            return null;
        }
    }

    /**
     * FALLBACK INTEGER GETTER
     * Returns matching configuration primitive, or falls back to default if absent or corrupted.
     */
    public static int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * ERROR MESSAGE TRANSLATOR
     * Returns the user-facing localized string. Returns null if string mapping is completely missing from database.
     */
    public static String getErrorMessage(String errorCode) {
        return errorMessages.get(errorCode);
    }
}