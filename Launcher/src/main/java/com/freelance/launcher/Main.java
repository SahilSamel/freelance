package com.freelance.launcher;

import com.freelance.config.ConfigurationEngine;
import com.freelance.config.DatabaseConfig;
import com.freelance.endpoint.login.Login;
import com.freelance.flyway.FlywayMigrationRunner;
import com.freelance.jpa.JpaFactory;
import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManagerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {
    public static void main(String[] args) {
        // 1. Parse startup configuration
        DatabaseConfig databaseConfig = DatabaseConfig.fromArgs(args);

        // 2. Run Flyway migrations
        FlywayMigrationRunner flywayMigrationRunner = new FlywayMigrationRunner();
        flywayMigrationRunner.migrate(databaseConfig);
        System.out.println("Database migration completed.");

        // 3. Create the long-lived factory
        JpaFactory jpaFactory = new JpaFactory();
        EntityManagerFactory entityManagerFactory = jpaFactory.createEntityManagerFactory(
                databaseConfig,
                "com.freelance.entity"
        );
        System.out.println("EntityManagerFactory created successfully.");

        // ─── CRITICAL STEP: INITIALIZE YOUR TRANSACTION ENGINE HERE ───
        // Pass the factory to your ThreadLocal manager ONCE at startup
        Transaction.init(entityManagerFactory);
        System.out.println("Transaction framework initialized.");

        // 4. Start your actual web server/router loop (Javalin, Netty, Helidon, etc.)
        // This keeps the application alive and listening for worker threads!
        bootHttpServer();

        ConfigurationEngine.initializeFromDatabase(entityManagerFactory);

        // 5. Add a Shutdown Hook to close the factory gracefully when the app stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
                System.out.println("EntityManagerFactory closed gracefully.");
            }
        }));
    }

    private static void bootHttpServer() {
        String baseUri = "http://localhost:8080/";

        try {
            // 1. Verify the class loader can actually see and load the class
            System.out.println("[DEBUG] 1/4 Attempting to reference endpoint.login.login.class...");
            Class<?> loginClass = Login.class;
            System.out.println("[DEBUG] Success! Class loaded. Canonical Name: " + loginClass.getCanonicalName());

            // 2. Quick sanity check for JAX-RS annotations
            System.out.println("[DEBUG] 2/4 Checking for JAX-RS annotations...");
            boolean hasPath = loginClass.isAnnotationPresent(jakarta.ws.rs.Path.class)
                    || loginClass.isAnnotationPresent(javax.ws.rs.Path.class);
            if (hasPath) {
                System.out.println("[DEBUG] Found @Path annotation on login class.");
            } else {
                System.out.println("[WARNING] login class does NOT have a @Path annotation. Jersey might ignore it!");
            }

            // 3. Registering resources
            System.out.println("[DEBUG] 3/4 Registering resources with ResourceConfig...");

            // NOTE: Fixed package path from "com.freelance.jaxrs" to "com.freelance.rest.jaxrs"
            // to perfectly match your ExceptionHandler and TransactionFilter packages.
            ResourceConfig config = new ResourceConfig()
                    .packages("com.freelance.endpoint", "com.freelance.rest.jaxrs")
                    .register(org.glassfish.jersey.jackson.JacksonFeature.class);
            System.out.println("[DEBUG] ResourceConfig configuration complete.");

            // 4. Starting the server
            System.out.println("[DEBUG] 4/4 Starting embedded HTTP server on " + baseUri);
            HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), config);

            // ─── EXTRA DEBUG LOGGING: PRINT ALL DISCOVERED COMPONENTS ───
            System.out.println("\n=========================================================");
            System.out.println("   JERSEY REGISTRY INSPECTOR (Post-Initialization)");
            System.out.println("=========================================================");

            System.out.println("\n[ACTIVE COMPONENTS & PROVIDERS]:");
            if (config.getClasses().isEmpty()) {
                System.out.println("  -> (None found via package scanning yet)");
            } else {
                for (Class<?> registeredClass : config.getClasses()) {
                    System.out.println("  -> [Class] " + registeredClass.getName());
                }
            }

            System.out.println("\n[ACTIVE SINGLETONS / FEATURE INSTANCES]:");
            if (config.getInstances().isEmpty()) {
                System.out.println("  -> (No direct instances registered)");
            } else {
                for (Object instance : config.getInstances()) {
                    System.out.println("  -> [Instance] " + instance.getClass().getName());
                }
            }
            System.out.println("=========================================================\n");

            System.out.println("Application started and listening on port 8080...");

        } catch (NoClassDefFoundError e) {
            System.err.println("\n[CRITICAL ERROR] Dependency missing at runtime! The JVM knows about the class but can't find its compiled bytecode or a dependency it relies on.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n[ERROR] Standard Exception thrown during initialization:");
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("\n[CRITICAL ERROR] Severe Throwable caught (Likely an InitializationError or LinkageError):");
            t.printStackTrace();
        }
    }
}
