package com.freelance.jpa;

import jakarta.persistence.Entity;
import org.reflections.Reflections;

import java.util.Set;

public class EntityScanner {

    public Set<Class<?>> scanEntities(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        return reflections.getTypesAnnotatedWith(Entity.class);
    }
}