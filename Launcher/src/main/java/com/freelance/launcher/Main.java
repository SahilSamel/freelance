package com.freelance.launcher;

import com.freelance.ConfigurationEngine;
import com.freelance.config.DatabaseConfig;
import com.freelance.flyway.FlywayMigrationRunner;
import com.freelance.jpa.JpaFactory;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManagerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Application entry point. Boots the database, JPA, transaction
 * framework, configuration cache, and embedded HTTP server, in that order.
 */
public class Main {

    private static final String CLASS_NAME = Main.class.getName();
    private static final Logger logger = LoggerFactory.getLogger(CLASS_NAME);
    private static final Map<String, String> RESOURCE_PACKAGE_LABELS = new LinkedHashMap<>();

    static {
        RESOURCE_PACKAGE_LABELS.put("com.freelance.endpoint", "endpoints");
        RESOURCE_PACKAGE_LABELS.put("com.freelance.rest.jaxrs", "providers");
    }

    /**
     * Runs the full application startup sequence: parses config, applies
     * database migrations, builds the JPA factory, initializes the
     * transaction framework, loads the configuration cache, and only then
     * starts accepting HTTP traffic.
     */
    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = null;
        HttpServer httpServer = null;

        try {
            DatabaseConfig databaseConfig = DatabaseConfig.fromArgs(args);

            FlywayMigrationRunner flywayMigrationRunner = new FlywayMigrationRunner();
            flywayMigrationRunner.migrate(databaseConfig);

            JpaFactory jpaFactory = new JpaFactory();
            entityManagerFactory = jpaFactory.createEntityManagerFactory(
                    databaseConfig,
                    "com.freelance.entity"
            );

            Transaction.init(entityManagerFactory);

            CacheManager.getInstance().init("com.freelance.cache.config", entityManagerFactory);

            ConfigurationEngine.init(entityManagerFactory);

            httpServer = bootHttpServer();

            final EntityManagerFactory emfForHook = entityManagerFactory;
            final HttpServer serverForHook = httpServer;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (serverForHook != null && serverForHook.isStarted()) {
                    serverForHook.shutdownNow();
                }
                if (emfForHook.isOpen()) {
                    emfForHook.close();
                }
            }));

        } catch (Exception e) {
            logger.error("[{}] Fatal error during application startup. Shutting down.", CLASS_NAME, e);
            if (httpServer != null && httpServer.isStarted()) {
                httpServer.shutdownNow();
            }
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
            }

            System.exit(1);
        }
    }

    /**
     * Configures and starts the embedded Grizzly/Jersey HTTP server,
     * registering all JAX-RS resources and providers found under the
     * application's endpoint and framework packages.
     *
     * @return the started HttpServer instance, or null if startup failed
     */
    private static HttpServer bootHttpServer() {
        String baseUri = "http://localhost:8080/";

        ResourceConfig config = new ResourceConfig()
                .packages(RESOURCE_PACKAGE_LABELS.keySet().toArray(new String[0]))
                .register(org.glassfish.jersey.jackson.JacksonFeature.class);

        if (logger.isDebugEnabled()) {
            logResourceSummary(config.getClasses());
        }

        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), config);
    }

    /**
     * Groups registered resource classes by their sub-package (relative to
     * the scanned base package) and logs a count per group, e.g.
     * "Registered 3 endpoints from login".
     */
    private static void logResourceSummary(Set<Class<?>> registeredClasses) {
        Map<String, Map<String, Integer>> counts = new TreeMap<>();

        for (Class<?> clazz : registeredClasses) {
            String packageName = clazz.getPackageName();

            RESOURCE_PACKAGE_LABELS.entrySet().stream()
                    .filter(entry -> packageName.equals(entry.getKey()) || packageName.startsWith(entry.getKey() + "."))
                    .findFirst()
                    .ifPresent(entry -> {
                        String basePackage = entry.getKey();
                        String label = entry.getValue();
                        String relativePackage = packageName.equals(basePackage)
                                ? "(root)"
                                : packageName.substring(basePackage.length() + 1);

                        counts.computeIfAbsent(label, k -> new TreeMap<>())
                                .merge(relativePackage, 1, Integer::sum);
                    });
        }

        counts.forEach((label, byPackage) ->
                byPackage.forEach((relativePackage, count) ->
                        logger.debug("[{}] Registered {} {} from {}", CLASS_NAME, count, label, relativePackage)));
    }
}