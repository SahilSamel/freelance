package com.freelance.jpa;

import com.freelance.config.DatabaseConfig;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds the application's {@link EntityManagerFactory} by scanning for
 * JPA entities and bootstrapping Hibernate programmatically (no
 * {@code persistence.xml}).
 */
public class JpaFactory {

    private static final String CLASS_NAME = JpaFactory.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(JpaFactory.class);

    /**
     * Scans the given package for {@code @Entity} classes and builds a
     * Hibernate-backed {@link EntityManagerFactory} against the provided
     * database configuration.
     *
     * @param databaseConfig    connection settings for the target database
     * @param entityBasePackage root package to scan for JPA entities
     * @return a ready-to-use EntityManagerFactory
     * @throws IllegalStateException if no entities are found in the given package
     */
    public EntityManagerFactory createEntityManagerFactory(
            DatabaseConfig databaseConfig,
            String entityBasePackage
    ) {

        EntityScanner entityScanner = new EntityScanner();
        Set<Class<?>> entityClasses = entityScanner.scanEntities(entityBasePackage);

        if (entityClasses.isEmpty()) {
            throw new IllegalStateException(
                    "No JPA entities found in package: " + entityBasePackage
            );
        }

        if (logger.isDebugEnabled()) {
            logEntitySummary(entityBasePackage, entityClasses);
        }

        StandardServiceRegistry serviceRegistry =
                new StandardServiceRegistryBuilder()
                        // Build Hibernate programmatically because this project does not use persistence.xml.
                        .applySetting("jakarta.persistence.jdbc.url", databaseConfig.url())
                        .applySetting("jakarta.persistence.jdbc.user", databaseConfig.username())
                        .applySetting("jakarta.persistence.jdbc.password", databaseConfig.password())
                        .applySetting("jakarta.persistence.jdbc.driver", "org.postgresql.Driver")
                        .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")

                        // Flyway owns schema creation.
                        .applySetting("hibernate.hbm2ddl.auto", "validate")

                        .applySetting("hibernate.show_sql", "true")
                        .applySetting("hibernate.format_sql", "true")
                        .build();

        MetadataSources metadataSources = new MetadataSources(serviceRegistry);

        for (Class<?> entityClass : entityClasses) {
            metadataSources.addAnnotatedClass(entityClass);
        }

        return metadataSources
                .buildMetadata()
                .buildSessionFactory();
    }

    /**
     * Groups discovered entity classes by their sub-package (relative to
     * the scanned base package) and logs a count per group, e.g.
     * "Registered 4 entities from user".
     */
    private void logEntitySummary(String basePackage, Set<Class<?>> entityClasses) {
        Map<String, Integer> countsByPackage = new TreeMap<>();

        for (Class<?> entityClass : entityClasses) {
            String packageName = entityClass.getPackageName();
            String relativePackage = packageName.equals(basePackage)
                    ? "(root)"
                    : packageName.substring(basePackage.length() + 1);

            countsByPackage.merge(relativePackage, 1, Integer::sum);
        }

        countsByPackage.forEach((relativePackage, count) ->
                logger.debug("[{}] Registered {} entities from {}", CLASS_NAME, count, relativePackage));
    }
}