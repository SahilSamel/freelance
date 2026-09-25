package com.freelance.initialization;

import com.freelance.auth.token.JwtSecretManager;
import com.freelance.bootstrap.ControlPlaneBootstrap;
import com.freelance.config.DatabaseConfig;
import com.freelance.config.CacheRegistry;
import com.freelance.enums.DeploymentMode;
import com.freelance.event.engine.EventRegistry;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.dto.TenantOnboardResponseDTO;
import com.freelance.tenant.onboarding.TenantOnboardingService;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Init {

    private static final Logger logger = LoggerFactory.getLogger(Init.class);
    private static volatile TenantOnboardResponseDTO defaultTenantResult;

    public static void startUp(EntityManagerFactory entityManagerFactory, DatabaseConfig databaseConfig) {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            throw new IllegalStateException("FATAL: EntityManagerFactory is null or closed. Cannot initialize application startup!");
        }

        try {
            // 1. Initialize Global Engines
            Transaction.init(entityManagerFactory);
            JwtSecretManager.initialize();
            CacheRegistry.getInstance().init();
            EventRegistry.getInstance().init();

            // 2. Initialize Control Plane
            if (databaseConfig != null) {
                ControlPlaneBootstrap.bootstrap(databaseConfig);

                // In TEST mode, provision a pristine test customer tenant for integration tests
                if (databaseConfig.mode() == DeploymentMode.TEST) {
                    TenantOnboardingService onboardingService = new TenantOnboardingService();
                    defaultTenantResult = onboardingService.onboardTenant(
                            new TenantCreateRequestDTO("testtenant", "Test Customer Tenant")
                    );
                    logger.info("[Init] TEST MODE: Provisioned test customer tenant 'testtenant'.");
                }
            }

            logger.info("[Init] Application server engines initialized successfully.");
        } catch (Exception e) {
            throw new IllegalStateException("FATAL: Critical failure during application startup in Init.startUp()! Server startup aborted.", e);
        }
    }

    public static void startUp(EntityManagerFactory entityManagerFactory) {
        startUp(entityManagerFactory, null);
    }

    public static TenantOnboardResponseDTO getDefaultTenantResult() {
        return defaultTenantResult;
    }
}
