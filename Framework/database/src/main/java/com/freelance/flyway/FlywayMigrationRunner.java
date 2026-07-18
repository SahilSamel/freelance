package com.freelance.flyway;

import com.freelance.config.DatabaseConfig;
import org.flywaydb.core.Flyway;

/**
 * Runs Flyway database migrations at application boot, ensuring the
 * schema is up to date before the app starts serving requests.
 */
public class FlywayMigrationRunner {

    /**
     * Applies any pending Flyway migrations to the target database.
     *
     * @param databaseConfig connection settings for the target database
     */
    public void migrate(DatabaseConfig databaseConfig) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        databaseConfig.url(),
                        databaseConfig.username(),
                        databaseConfig.password()
                )
                .locations("classpath:db/migration/*")
                .load();

        flyway.migrate();
    }
}