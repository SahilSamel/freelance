package com.freelance.jpa;

import com.freelance.config.DatabaseConfig;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.Set;

public class JpaFactory {

    public EntityManagerFactory createEntityManagerFactory(
            DatabaseConfig databaseConfig,
            String entityBasePackage
    ) {
        EntityScanner entityScanner = new EntityScanner();
        // Scan before bootstrapping Hibernate so missing entity packages fail loudly during startup.
        Set<Class<?>> entityClasses = entityScanner.scanEntities(entityBasePackage);

        if (entityClasses.isEmpty()) {
            throw new IllegalStateException(
                    "No JPA entities found in package: " + entityBasePackage
            );
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

        // Register every discovered entity so Hibernate can validate mappings against the migrated schema.
        for (Class<?> entityClass : entityClasses) {
            metadataSources.addAnnotatedClass(entityClass);
        }

        SessionFactory sessionFactory = metadataSources
                .buildMetadata()
                .buildSessionFactory();

        return sessionFactory;
    }
}
