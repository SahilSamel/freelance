package launcher;

import config.ConfigurationEngine;
import config.DatabaseConfig;
import endpoint.Login.Login;
import flyway.FlywayMigrationRunner;
import jakarta.persistence.EntityManagerFactory;
import jpa.JpaFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import transaction.Transaction;

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
                "entity"
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
            System.out.println("[DEBUG] 1/4 Attempting to reference endpoint.Login.Login.class...");
            Class<?> loginClass = Login.class;
            System.out.println("[DEBUG] Success! Class loaded. Canonical Name: " + loginClass.getCanonicalName());

            // 2. Quick sanity check for JAX-RS annotations
            System.out.println("[DEBUG] 2/4 Checking for JAX-RS annotations...");
            boolean hasPath = loginClass.isAnnotationPresent(jakarta.ws.rs.Path.class)
                    || loginClass.isAnnotationPresent(javax.ws.rs.Path.class);
            if (hasPath) {
                System.out.println("[DEBUG] Found @Path annotation on Login class.");
            } else {
                System.out.println("[WARNING] Login class does NOT have a @Path annotation. Jersey might ignore it!");
            }

            // 3. Registering resources
            System.out.println("[DEBUG] 3/4 Registering resources with ResourceConfig...");
            ResourceConfig config = new ResourceConfig()
                    .packages("endpoint")
                    .register(org.glassfish.jersey.jackson.JacksonFeature.class);
            System.out.println("[DEBUG] ResourceConfig configuration complete.");

            // 4. Starting the server
            System.out.println("[DEBUG] 4/4 Starting embedded HTTP server on " + baseUri);
            HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), config);

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
