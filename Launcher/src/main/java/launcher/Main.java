package launcher;

import config.DatabaseConfig;
import flyway.FlywayMigrationRunner;
import jpa.JpaFactory;
import jakarta.persistence.EntityManagerFactory;

public class Main {
    public static void main(String[] args) {
        // Parse startup configuration once and pass the immutable record through the boot sequence.
        DatabaseConfig databaseConfig = DatabaseConfig.fromArgs(args);

        // Run migrations before creating JPA so Hibernate validates against the latest schema.
        FlywayMigrationRunner flywayMigrationRunner = new FlywayMigrationRunner();
        flywayMigrationRunner.migrate(databaseConfig);
        System.out.println("Database migration completed.");
        System.out.println("Application started.");

        JpaFactory jpaFactory = new JpaFactory();

        // Scan the modules package for entities owned by feature modules.
        EntityManagerFactory entityManagerFactory =
                jpaFactory.createEntityManagerFactory(
                        databaseConfig,
                        "Freelance.modules"
                );

        System.out.println("EntityManagerFactory created successfully.");

        // This launcher currently verifies startup wiring, then releases the factory immediately.
        entityManagerFactory.close();


    }
}
