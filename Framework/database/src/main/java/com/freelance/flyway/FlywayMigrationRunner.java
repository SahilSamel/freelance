package com.freelance.flyway;

import com.freelance.config.DatabaseConfig;
import org.flywaydb.core.Flyway;

public class FlywayMigrationRunner {

    public void migrate(DatabaseConfig databaseConfig) {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        databaseConfig.url(),
                        databaseConfig.username(),
                        databaseConfig.password()
                )
                .schemas("freelance")
                .defaultSchema("freelance")
                .createSchemas(true)
                .cleanDisabled(false)
                .locations("classpath:db/migration")
                .load();

        flyway.repair();
        flyway.migrate();
    }
}