package com.freelance.initialization;

import com.freelance.authorization.seeder.AuthorizationSeeder;
import com.freelance.configuration.error.ErrorConfigurationService;
import com.freelance.configuration.seeder.ConfigurationSeeder;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.roleAuthorization.RoleAuthorizationService;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;
import jakarta.persistence.EntityManagerFactory;

public class Init {

    /**
     * Executes the bootstrap sequence for the application.
     * <p>
     * Initializes the {@link Transaction} manager, seeds default system/error configurations and authorization roles
     * into the database, initializes {@link CacheManager}, and preloads system config, error message, and role authorization caches.
     * </p>
     *
     * @param entityManagerFactory the open JPA entity manager factory; must not be null
     * @throws IllegalStateException if {@code entityManagerFactory} is null/closed or if startup initialization fails
     */
    public static void startUp(EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            throw new IllegalStateException("FATAL: EntityManagerFactory is null or closed. Cannot initialize application startup!");
        }

        try {
            Transaction.init(entityManagerFactory);

            try (Transaction tx = Transaction.start()) {

                //Setting the userId as System UUID so that all automatically seeded entries are marked as such
                Transaction.getRequestContext().withUserId(SystemHardcoding.SYSTEM_USER_ID.asUuid());

                ConfigurationSeeder configurationSeeder = new ConfigurationSeeder();
                configurationSeeder.seed();

                AuthorizationSeeder authorizationSeeder = new AuthorizationSeeder();
                authorizationSeeder.seed();

                CacheManager.getInstance().init("com.freelance.cache.config");

                SystemConfigurationService systemConfigurationService = new SystemConfigurationService();
                systemConfigurationService.preloadSystemCacheEntries();

                ErrorConfigurationService errorConfigurationService = new ErrorConfigurationService();
                errorConfigurationService.preloadErrorCacheEntries();

                RoleAuthorizationService roleAuthorizationService = new RoleAuthorizationService();
                roleAuthorizationService.preloadRoleAuthorizationCacheEntries();
            }
        } catch (Exception e) {
            throw new IllegalStateException("FATAL: Critical failure during application startup in Init.startUp()! Server startup aborted.", e);
        }
    }
}
