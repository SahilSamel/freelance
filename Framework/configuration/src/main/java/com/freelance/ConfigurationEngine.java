package com.freelance;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.manager.CacheManager;

/**
 * Facade over the application's config-property and keys-message caches.
 * Ensures consistent lookups driven by IConfig and IError contracts with code-level defaults.
 */
public final class ConfigurationEngine {

    private ConfigurationEngine() {
    }

    /**
     * Returns the string value for the given IConfig, or its canonical default if absent or blank in cache/DB.
     */
    public static String get(IConfig config) {
        if (config == null) return null;
        String value = CacheManager.getInstance().get("cache_SystemConfig", config.getKey());
        return (value != null && !value.isBlank()) ? value : config.getDefaultValue();
    }

    /**
     * Returns the int value for the given IConfig, parsing the configured value or falling back to default.
     */
    public static int getInt(IConfig config) {
        String value = get(config);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return Integer.parseInt(config.getDefaultValue().trim());
        }
    }

    /**
     * Returns the long value for the given IConfig, parsing the configured value or falling back to default.
     */
    public static long getLong(IConfig config) {
        String value = get(config);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return Long.parseLong(config.getDefaultValue().trim());
        }
    }

    /**
     * Returns the boolean value for the given IConfig, parsing the configured value or falling back to default.
     */
    public static boolean getBoolean(IConfig config) {
        String value = get(config);
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Returns the HTTP status code for the given IError, or its default status code if unconfigured.
     */
    public static int getStatusCode(IError error) {
        if (error == null) return 500;
        ErrorConfigEntry entry = CacheManager.getInstance().get("cache_ErrorMessage", error.getErrorKey());
        return (entry != null && entry.statusCode() > 0)
                ? entry.statusCode()
                : error.getDefaultStatusCode();
    }

    /**
     * Returns the user-facing localized message for the given IError, or its default message if unconfigured.
     */
    public static String getErrorMessage(IError error) {
        if (error == null) return null;
        ErrorConfigEntry entry = CacheManager.getInstance().get("cache_ErrorMessage", error.getErrorKey());
        return (entry != null && entry.errorMessage() != null && !entry.errorMessage().isBlank())
                ? entry.errorMessage()
                : error.getDefaultMessage();
    }

    /**
     * Forces a single config key to reload from the database on its next lookup.
     */
    public static void invalidateConfig(IConfig config) {
        if (config != null) {
            CacheManager.getInstance().invalidate("cache_SystemConfig", config.getKey());
        }
    }

    /**
     * Forces a single key to reload from the database on its next lookup.
     */
    public static void invalidateError(IError error) {
        if (error != null) {
            CacheManager.getInstance().invalidate("cache_ErrorMessage", error.getErrorKey());
        }
    }
}