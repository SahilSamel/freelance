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

public class JpaFactory {

    private static final String CLASS_NAME = JpaFactory.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(JpaFactory.class);

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
                        .applySetting("jakarta.persistence.jdbc.url", databaseConfig.url())
                        .applySetting("jakarta.persistence.jdbc.user", databaseConfig.username())
                        .applySetting("jakarta.persistence.jdbc.password", databaseConfig.password())
                        .applySetting("jakarta.persistence.jdbc.driver", "org.postgresql.Driver")
                        .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                        .applySetting("hibernate.hbm2ddl.auto", "none")
                        .applySetting("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")
                        .applySetting("hibernate.hikari.maximumPoolSize", "60")
                        .applySetting("hibernate.hikari.minimumIdle", "10")
                        .applySetting("hibernate.hikari.idleTimeout", "30000")
                        .applySetting("hibernate.hikari.connectionTimeout", "20000")
                        .applySetting("hibernate.hikari.maxLifetime", "1800000")
                        .build();

        MetadataSources metadataSources = new MetadataSources(serviceRegistry);

        for (Class<?> entityClass : entityClasses) {
            metadataSources.addAnnotatedClass(entityClass);
        }

        return metadataSources
                .buildMetadata()
                .buildSessionFactory();
    }

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