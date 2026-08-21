package com.freelance.configuration.seeder;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Central registry of all IConfig and IError enums in the application.
 */
public final class ConfigurationRegistry {

    private static final String BASE_PACKAGE = "com.freelance.keys";
    private static final Reflections REFLECTIONS = new Reflections(BASE_PACKAGE, Scanners.SubTypes);

    private ConfigurationRegistry() {
    }

    /**
     * Finds and retrieves all enum constants implementing IConfig in com.freelance.keys.
     */
    public static List<IConfig> getAllConfigs() {
        return getEnumImplementations(IConfig.class);
    }

    /**
     * Finds and retrieves all enum constants implementing IError in com.freelance.keys.
     */
    public static List<IError> getAllErrors() {
        return getEnumImplementations(IError.class);
    }

    /**
     * Generic helper method to scan and collect enum constants of a given interface.
     */
    private static <T> List<T> getEnumImplementations(Class<T> interfaceClass) {
        List<T> resultList = new ArrayList<>();
        Set<Class<? extends T>> implementations = REFLECTIONS.getSubTypesOf(interfaceClass);

        for (Class<? extends T> clazz : implementations) {
            // Verify that the implementing type is an Enum
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