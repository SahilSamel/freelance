package com.freelance.flyway;

import com.freelance.config.DatabaseConfig;
import com.freelance.enums.DeploymentMode;
import com.freelance.system.SystemHardcoding;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlywayMigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(FlywayMigrationRunner.class);

    public void migrate(DatabaseConfig databaseConfig, String tenantSchema) {
        String schema = (tenantSchema != null && !tenantSchema.isBlank())
                ? tenantSchema.trim().toLowerCase()
                : SystemHardcoding.DEFAULT_TENANT.asString();

        boolean isControlPlane = SystemHardcoding.DEFAULT_TENANT.asString().equalsIgnoreCase(schema);
        String[] locations = isControlPlane
                ? new String[]{"classpath:db/migration/tenant", "classpath:db/migration/control"}
                : new String[]{"classpath:db/migration/tenant"};

        DeploymentMode mode = (databaseConfig != null && databaseConfig.mode() != null)
                ? databaseConfig.mode()
                : DeploymentMode.PROD;

        switch (mode) {
            case TEST -> {
                Flyway flyway = Flyway.configure()
                        .dataSource(databaseConfig.url(), databaseConfig.username(), databaseConfig.password())
                        .schemas(schema)
                        .defaultSchema(schema)
                        .createSchemas(true)
                        .cleanDisabled(false)
                        .locations(locations)
                        .load();

                logger.info("🧪 [TEST MODE] Cleaning database schema '{}' for pristine Day-0 onboarding...", schema);
                flyway.clean();
                flyway.repair();
                flyway.migrate();
            }
            case PROD -> {
                Flyway flyway = Flyway.configure()
                        .dataSource(databaseConfig.url(), databaseConfig.username(), databaseConfig.password())
                        .schemas(schema)
                        .defaultSchema(schema)
                        .createSchemas(true)
                        .cleanDisabled(true)
                        .locations(locations)
                        .load();

                flyway.repair();
                flyway.migrate();
            }
        }

        logger.info("[FlywayMigrationRunner] Successfully migrated database schema '{}'.", schema);
    }
}