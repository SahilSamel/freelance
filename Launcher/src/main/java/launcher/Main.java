package launcher;

import framework.database.DatabaseConfig;
import framework.database.FlywayMigrationRunner;

public class Main {
    public static void main(String[] args) {
        DatabaseConfig databaseConfig = DatabaseConfig.fromArgs(args);

        FlywayMigrationRunner flywayMigrationRunner = new FlywayMigrationRunner();
        flywayMigrationRunner.migrate(databaseConfig);

        System.out.println("Database migration completed.");
        System.out.println("Application started.");
    }
}
