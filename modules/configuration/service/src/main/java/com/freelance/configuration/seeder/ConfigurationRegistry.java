package com.freelance.configuration.seeder;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class ConfigurationRegistry {

    private static final String BASE_PACKAGE = "com.freelance.keys";
    private static final Reflections REFLECTIONS = new Reflections(BASE_PACKAGE, Scanners.SubTypes);

    private ConfigurationRegistry() {
    }

    public static List<IConfig> getAllConfigs() {
        return getEnumImplementations(IConfig.class);
    }

    public static List<IError> getAllErrors() {
        return getEnumImplementations(IError.class);
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