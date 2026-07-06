package com.freelance.jpa;

import jakarta.persistence.Entity;
import org.reflections.Reflections;

import java.util.Set;

public class EntityScanner {

    public Set<Class<?>> scanEntities(String basePackage) {
        System.out.println("\n[EntityScanner] 🔍 Initiating classpath scan for package: \"" + basePackage + "\"");

        // Discover entities across modules without requiring each module to register classes manually.
        Reflections reflections = new Reflections(basePackage);

        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);

        // ─── SYSTEM OUT LOGGING ──────────────────────────────────────────────
        System.out.println("[EntityScanner] ⏹️ Scan complete.");

        if (entities == null || entities.isEmpty()) {
            System.out.println("[EntityScanner] ⚠️ WARNING: zero @Entity classes detected in package \"" + basePackage + "\".");
            System.out.println("[EntityScanner] Current ClassLoader URLs checked: " +
                    Thread.currentThread().getContextClassLoader().toString());
        } else {
            System.out.println("[EntityScanner] 🎉 Success! Found " + entities.size() + " entity class(es):");
            for (Class<?> entityClass : entities) {
                System.out.println("   └── 👉 " + entityClass.getName());
            }
            System.out.println(); // Clean spacing line
        }
        // ─────────────────────────────────────────────────────────────────────

        return entities;
    }
}