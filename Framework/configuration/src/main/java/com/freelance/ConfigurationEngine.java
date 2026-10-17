package com.freelance;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.manager.CacheManager;
import com.freelance.manager.TenantCacheKeyResolver;

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
        try {
            String value = CacheManager.get("cache_SystemConfig", config.getKey());
            return (value != null && !value.isBlank()) ? value : config.getDefaultValue();
        } catch (IllegalStateException e) {
            return config.getDefaultValue();
        }
    }

    public static String get(IConfig config, String tenantId) {
        if (config == null) return null;
        if (tenantId == null || tenantId.isBlank()) {
            return get(config);
        }
        String previousTenant = null;
        try {
            if (TenantCacheKeyResolver.getInstance().getTenantSupplier() != null) {
                previousTenant = TenantCacheKeyResolver.getInstance().getTenantSupplier().get();
            }
            TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> tenantId);
            return get(config);
        } finally {
            final String restoreTenant = previousTenant;
            TenantCacheKeyResolver.getInstance().setTenantSupplier(() -> restoreTenant);
        }
    }

    public static int getInt(IConfig config) {
        String value = get(config);
        try {
            if (value != null && !value.isBlank()) {
                return Integer.parseInt(value.trim());
            }
        } catch (Exception ignored) {
        }
        try {
            return config != null && config.getDefaultValue() != null
                    ? Integer.parseInt(config.getDefaultValue().trim())
                    : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getLong(IConfig config) {
        String value = get(config);
        try {
            if (value != null && !value.isBlank()) {
                return Long.parseLong(value.trim());
            }
        } catch (Exception ignored) {
        }
        try {
            return config != null && config.getDefaultValue() != null
                    ? Long.parseLong(config.getDefaultValue().trim())
                    : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public static double getDouble(IConfig config) {
        return getDouble(config, 0.0);
    }

    public static double getDouble(IConfig config, double fallback) {
        return getDouble(config, fallback, null);
    }

    public static double getDouble(IConfig config, double fallback, String tenantId) {
        String value = get(config, tenantId);
        try {
            if (value != null && !value.isBlank()) {
                return Double.parseDouble(value.trim());
            }
        } catch (Exception ignored) {
        }
        try {
            return config != null && config.getDefaultValue() != null
                    ? Double.parseDouble(config.getDefaultValue().trim())
                    : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean getBoolean(IConfig config) {
        String value = get(config);
        if (value == null || value.isBlank()) {
            return config != null && config.getDefaultValue() != null && Boolean.parseBoolean(config.getDefaultValue().trim());
        }
        return Boolean.parseBoolean(value.trim());
    }

    public static <E extends Enum<E>> E getEnum(IConfig config, Class<E> enumClass) {
        if (config == null || enumClass == null) return null;
        String value = get(config);
        if (value != null && !value.isBlank()) {
            try {
                return Enum.valueOf(enumClass, value.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (config.getDefaultValue() != null && !config.getDefaultValue().isBlank()) {
            try {
                return Enum.valueOf(enumClass, config.getDefaultValue().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public static <E extends Enum<E>> E getEnum(IConfig config, Class<E> enumClass, E fallback) {
        E result = getEnum(config, enumClass);
        return result != null ? result : fallback;
    }

    public static int getStatusCode(IError error) {
        if (error == null) return 500;
        try {
            ErrorConfigEntry entry = CacheManager.get("cache_ErrorMessage", error.getErrorKey());
            return (entry != null && entry.statusCode() > 0)
                    ? entry.statusCode()
                    : error.getDefaultStatusCode();
        } catch (IllegalStateException e) {
            return error.getDefaultStatusCode();
        }
    }

    public static String getErrorMessage(IError error) {
        if (error == null) return null;
        try {
            ErrorConfigEntry entry = CacheManager.get("cache_ErrorMessage", error.getErrorKey());
            return (entry != null && entry.errorMessage() != null && !entry.errorMessage().isBlank())
                    ? entry.errorMessage()
                    : error.getDefaultMessage();
        } catch (IllegalStateException e) {
            return error.getDefaultMessage();
        }
    }

    public static void invalidateConfig(IConfig config) {
        if (config != null) {
            CacheManager.invalidate("cache_SystemConfig", config.getKey());
        }
    }

    public static void invalidateError(IError error) {
        if (error != null) {
            CacheManager.invalidate("cache_ErrorMessage", error.getErrorKey());
        }
    }

    public static boolean validate(String configKey, String value) {
        if (configKey == null || value == null) {
            return false;
        }
        IConfig config = com.freelance.registry.ConfigurationRegistry.getConfig(configKey);
        if (config != null) {
            String pattern = config.getValidation();
            if (pattern != null && !pattern.isBlank()) {
                return value.matches(pattern);
            }
        }
        return true;
    }

    public static String getValidationPattern(String configKey) {
        if (configKey == null) {
            return null;
        }
        IConfig config = com.freelance.registry.ConfigurationRegistry.getConfig(configKey);
        return config != null ? config.getValidation() : null;
    }
}