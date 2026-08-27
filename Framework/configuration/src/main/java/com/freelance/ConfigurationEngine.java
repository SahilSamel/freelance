package com.freelance;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.manager.CacheManager;

/**
 * Provides a unified static facade for retrieving runtime system configurations and error messages.
 * <p>
 * Queries the active {@link CacheManager} for dynamic database-backed overrides and falls back
 * transparently to the code-defined defaults in {@link IConfig} and {@link IError}.
 * </p>
 */
public final class ConfigurationEngine {

    private ConfigurationEngine() {

    }

    public static String get(IConfig config) {
        if (config == null) return null;
        String value = CacheManager.getInstance().get("cache_SystemConfig", config.getKey());
        return (value != null && !value.isBlank()) ? value : config.getDefaultValue();
    }

    public static int getInt(IConfig config) {
        String value = get(config);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return Integer.parseInt(config.getDefaultValue().trim());
        }
    }

    public static long getLong(IConfig config) {
        String value = get(config);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return Long.parseLong(config.getDefaultValue().trim());
        }
    }

    public static boolean getBoolean(IConfig config) {
        String value = get(config);
        return Boolean.parseBoolean(value.trim());
    }

    public static int getStatusCode(IError error) {
        if (error == null) return 500;
        ErrorConfigEntry entry = CacheManager.getInstance().get("cache_ErrorMessage", error.getErrorKey());
        return (entry != null && entry.statusCode() > 0)
                ? entry.statusCode()
                : error.getDefaultStatusCode();
    }

    public static String getErrorMessage(IError error) {
        if (error == null) return null;
        ErrorConfigEntry entry = CacheManager.getInstance().get("cache_ErrorMessage", error.getErrorKey());
        return (entry != null && entry.errorMessage() != null && !entry.errorMessage().isBlank())
                ? entry.errorMessage()
                : error.getDefaultMessage();
    }

    public static void invalidateConfig(IConfig config) {
        if (config != null) {
            CacheManager.getInstance().invalidate("cache_SystemConfig", config.getKey());
        }
    }

    public static void invalidateError(IError error) {
        if (error != null) {
            CacheManager.getInstance().invalidate("cache_ErrorMessage", error.getErrorKey());
        }
    }
}