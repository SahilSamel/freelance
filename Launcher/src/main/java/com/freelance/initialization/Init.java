package com.freelance.initialization;

import com.freelance.configuration.error.ErrorConfigurationService;
import com.freelance.configuration.seeder.ConfigurationSeeder;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.roleAuthorization.RoleAuthorizationService;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;
import jakarta.persistence.EntityManagerFactory;

public class Init {

    public static void startUp(EntityManagerFactory entityManagerFactory) {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            throw new IllegalStateException("FATAL: EntityManagerFactory is null or closed. Cannot initialize application startup!");
        }

        try {
            // Starting transaction management
            Transaction.init(entityManagerFactory);

            try (Transaction tx = Transaction.start()) {
                Transaction.getRequestContext().withUserId(SystemHardcoding.SYSTEM_USER_ID.asUuid());

                // 1. Seeding system configurations and keys messages from code defaults
                ConfigurationSeeder configurationSeeder = new ConfigurationSeeder();
                configurationSeeder.seed();

                // 2. Initializing Cache Management
                CacheManager.getInstance().init("com.freelance.cache.config");

                // 3. Preloading Configuration and Error cache
                SystemConfigurationService systemConfigurationService = new SystemConfigurationService();
                systemConfigurationService.preloadSystemCacheEntries();

                ErrorConfigurationService errorConfigurationService = new ErrorConfigurationService();
                errorConfigurationService.preloadErrorCacheEntries();

                // 4. Preloading AuthorizationCache
                RoleAuthorizationService roleAuthorizationService = new RoleAuthorizationService();
                roleAuthorizationService.preloadRoleAuthorizationCacheEntries();
            }
        } catch (Exception e) {
            throw new IllegalStateException("FATAL: Critical failure during application startup in Init.startUp()! Server startup aborted.", e);
        }
    }
}
