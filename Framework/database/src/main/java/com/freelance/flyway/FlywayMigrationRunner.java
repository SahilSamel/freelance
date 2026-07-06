package com.freelance.flyway;

import com.freelance.config.DatabaseConfig;
import org.flywaydb.core.Flyway;


public class FlywayMigrationRunner {
    public void migrate(DatabaseConfig databaseConfig) {
        // Keep migration scripts in the launcher resources so the app starts with the schema it expects.
        Flyway flyway = Flyway.configure()
                .dataSource(
                        databaseConfig.url(),
                        databaseConfig.username(),
                        databaseConfig.password()
                )
                .locations("classpath:db/migration")
                .load();

        // Flyway tracks applied versions and runs only pending migrations.
        flyway.migrate();
    }
}
