package com.freelance.flyway;

import com.freelance.config.DatabaseConfig;
import com.freelance.enums.DeploymentMode;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlywayMigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(FlywayMigrationRunner.class);

    public void migrate(DatabaseConfig databaseConfig) {
        boolean isTestMode = databaseConfig.mode() == DeploymentMode.TEST;
        boolean isProdMode = databaseConfig.mode() == DeploymentMode.PROD;

        Flyway flyway = Flyway.configure()
                .dataSource(
                        databaseConfig.url(),
                        databaseConfig.username(),
                        databaseConfig.password()
                )
                .schemas("freelance")
                .defaultSchema("freelance")
                .createSchemas(true)
                .cleanDisabled(isProdMode)
                .locations("classpath:db/migration")
                .load();

        if (isTestMode) {
            logger.info("🧪 [TEST MODE] Cleaning database schema for a pristine Day-0 deployment...");
            flyway.clean();
        }

        flyway.repair();
        flyway.migrate();
    }
}