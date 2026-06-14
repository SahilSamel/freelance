package jpa;

import jakarta.persistence.Entity;
import org.reflections.Reflections;

import java.util.Set;

public class EntityScanner {

    public Set<Class<?>> scanEntities(String basePackage) {
        // Discover entities across modules without requiring each module to register classes manually.
        Reflections reflections = new Reflections(basePackage);

        return reflections.getTypesAnnotatedWith(Entity.class);
    }
}
