package com.freelance.jpa;

import jakarta.persistence.Entity;
import org.reflections.Reflections;

import java.util.Set;

/**
 * Scans the classpath for JPA entity classes so they can be registered
 * without each module needing to declare them manually.
 */
public class EntityScanner {

    /**
     * Finds all classes annotated with {@link Entity} under the given package.
     *
     * @param basePackage the root package to scan
     * @return the set of discovered entity classes (may be empty)
     */
    public Set<Class<?>> scanEntities(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getTypesAnnotatedWith(Entity.class);
    }
}