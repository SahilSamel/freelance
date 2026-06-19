package launcher;

import config.ConfigurationEngine;
import config.DatabaseConfig;
import flyway.FlywayMigrationRunner;
import jakarta.persistence.EntityManagerFactory;
import jpa.JpaFactory;
import transaction.Transaction;

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
                "Freelance.modules"
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
        System.out.println("Application started and listening for incoming requests...");
        // Your server listening logic loop goes here.
        // Inside THAT loop is where the threads call Transaction.start() and finish()!
    }
}
