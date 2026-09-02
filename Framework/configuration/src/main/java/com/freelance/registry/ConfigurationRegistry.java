package com.freelance.registry;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans classpath package {@code com.freelance.keys} for {@link IConfig} and {@link IError} implementations
 * and maintains an indexed lookup by configuration key.
 */
public final class ConfigurationRegistry {

    private static final String BASE_PACKAGE = "com.freelance.keys";
    private static final Reflections REFLECTIONS = new Reflections(BASE_PACKAGE, Scanners.SubTypes);
    private static final Map<String, IConfig> CONFIGS_BY_KEY = new ConcurrentHashMap<>();

    private ConfigurationRegistry() {
    }

    public static List<IConfig> getAllConfigs() {
        return getEnumImplementations(IConfig.class);
    }

    public static List<IError> getAllErrors() {
        return getEnumImplementations(IError.class);
    }

    public static IConfig getConfig(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (CONFIGS_BY_KEY.isEmpty()) {
            for (IConfig config : getAllConfigs()) {
                if (config.getKey() != null) {
                    CONFIGS_BY_KEY.put(config.getKey(), config);
                }
            }
        }
        return CONFIGS_BY_KEY.get(key);
    }

    /**
     * Resets the internal cache. Intended for testing.
     */
    public static void resetCacheForTesting() {
        CONFIGS_BY_KEY.clear();
    }

    private static <T> List<T> getEnumImplementations(Class<T> interfaceClass) {
        List<T> resultList = new ArrayList<>();
        Set<Class<? extends T>> implementations = REFLECTIONS.getSubTypesOf(interfaceClass);

        for (Class<? extends T> clazz : implementations) {
            if (clazz.isEnum()) {
                T[] constants = clazz.getEnumConstants();
                if (constants != null) {
                    resultList.addAll(Arrays.asList(constants));
                }
            }
        }
        return resultList;
    }
}
