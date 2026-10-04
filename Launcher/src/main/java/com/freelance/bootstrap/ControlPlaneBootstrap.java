package com.freelance.bootstrap;

import com.freelance.authorization.entity.Authorization;
import com.freelance.authorization.repository.AuthorizationRepository;
import com.freelance.authorization.seeder.AuthorizationSeeder;
import com.freelance.config.DatabaseConfig;
import com.freelance.config.DatabaseConfigHolder;
import com.freelance.configuration.error.ErrorConfigurationService;
import com.freelance.configuration.seeder.ConfigurationSeeder;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.mapping.roleAuthorization.RoleAuthorizationService;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.mapping.roleAuthorization.repository.RoleAuthorizationRepository;
import com.freelance.mapping.userRole.entity.UserRoleEntity;
import com.freelance.mapping.userRole.repository.UserRoleRepository;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.jaxrs.tenant.TenantRegistry;
import com.freelance.role.entity.Role;
import com.freelance.role.repository.RoleRepository;
import com.freelance.system.SystemHardcoding;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.control.key.PlatformVendorKeyService;
import com.freelance.tenant.repository.TenantRepository;
import com.freelance.user.entity.User;
import com.freelance.user.repository.UserRepository;
import com.freelance.utility.password.HashUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class ControlPlaneBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(ControlPlaneBootstrap.class);

    public static final String CONTROL_ADMIN_USERNAME = "controladmin";
    public static final String CONTROL_ADMIN_DEFAULT_PASSWORD = "ControlAdmin@123";
    public static final String ROLE_CONTROL_ADMIN = "ROLE_CONTROL_ADMIN";

    public static String getControlAdminInitialPassword() {
        String envPassword = System.getenv("CONTROL_ADMIN_INITIAL_PASSWORD");
        if (envPassword != null && !envPassword.isBlank()) {
            return envPassword.trim();
        }
        return CONTROL_ADMIN_DEFAULT_PASSWORD;
    }

    private static final Set<String> CONTROL_ADMIN_PERMISSIONS = Set.of(
            "TENANT_CREATE",
            "TENANT_LIST",
            "TENANT_DELETE",
            "SUPPORT_VENDOR_SESSION",
            "SYSTEM_CONFIG_LIST",
            "SYSTEM_CONFIG_UPDATE",
            "AUDIT_LOG_LIST"
    );

    public static void bootstrap(DatabaseConfig databaseConfig) {
        if (databaseConfig != null) {
            DatabaseConfigHolder.set(databaseConfig);
        }

        String controlTenant = SystemHardcoding.DEFAULT_TENANT.asString();
        logger.info("================================================================================");
        logger.info("[ControlPlaneBootstrap] Initializing Control Plane ({})", controlTenant);
        logger.info("================================================================================");

        List<TenantEntity> allTenants = Collections.emptyList();
        try (Transaction tx = Transaction.start(controlTenant)) {
            Transaction.getRequestContext().withUserId(SystemHardcoding.SYSTEM_USER_ID.asUuid());

            // 1. Seed configurations & errors for control plane
            ConfigurationSeeder configSeeder = new ConfigurationSeeder();
            configSeeder.seed();

            // 2. Seed authorizations (discovers all @Authorization annotations)
            AuthorizationSeeder authSeeder = new AuthorizationSeeder();
            authSeeder.seed();

            // 3. Preload control plane caches
            new SystemConfigurationService().preloadSystemCacheEntries();
            new ErrorConfigurationService().preloadErrorCacheEntries();

            // 4. Ensure Platform Vendor RSA keypair is generated and stored in freelance.FL_SYSTEM_CONFIGURATION
            PlatformVendorKeyService keyService = new PlatformVendorKeyService();
            keyService.ensureKeysExist();

            // 5. Register freelance in FL_TENANTS if missing, and sync active tenants into TenantRegistry
            TenantRepository tenantRepo = new TenantRepository();
            if (!tenantRepo.existsByTenantId(controlTenant)) {
                TenantEntity controlEntity = new TenantEntity(controlTenant, "Control Plane", TenantRegistry.STATUS_ACTIVE);
                tenantRepo.persist(controlEntity);
                logger.info("[ControlPlaneBootstrap] Registered Control Plane in FL_TENANTS table.");
            }

            // Sync all tenants from database into TenantRegistry cache
            allTenants = tenantRepo.findAllTenants();
            for (TenantEntity t : allTenants) {
                TenantRegistry.registerTenant(t.getTenantId(), t.getStatus());
            }
            TenantRegistry.registerTenant(controlTenant, TenantRegistry.STATUS_ACTIVE);

            // 6. Ensure ROLE_CONTROL_ADMIN exists
            RoleRepository roleRepo = new RoleRepository();
            Role controlRole = roleRepo.findByName(ROLE_CONTROL_ADMIN);
            if (controlRole == null) {
                controlRole = new Role(UUID.randomUUID(), ROLE_CONTROL_ADMIN, "Platform Control Plane Administrator", true);
                controlRole.setCreatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
                controlRole.setCreatedDate(LocalDateTime.now());
                roleRepo.persist(controlRole);
                logger.info("[ControlPlaneBootstrap] Created role '{}'.", ROLE_CONTROL_ADMIN);
            }

            // 7. Map permissions to ROLE_CONTROL_ADMIN
            AuthorizationRepository authRepo = new AuthorizationRepository();
            RoleAuthorizationRepository roleAuthRepo = new RoleAuthorizationRepository();
            Map<String, Authorization> allAuthMap = authRepo.findAllAsMap();

            List<RoleAuthorization> existingMappings = roleAuthRepo.getRoleAuthorizationsByRoleId(controlRole.getId());
            Set<UUID> existingAuthIds = new HashSet<>();
            for (RoleAuthorization ra : existingMappings) {
                if (ra.getAuthorization() != null) {
                    existingAuthIds.add(ra.getAuthorization().getId());
                }
            }

            for (String permKey : CONTROL_ADMIN_PERMISSIONS) {
                Authorization auth = allAuthMap.get(permKey);
                if (auth != null && !existingAuthIds.contains(auth.getId())) {
                    RoleAuthorization ra = new RoleAuthorization(controlRole, auth);
                    ra.setCreatedDate(LocalDateTime.now());
                    roleAuthRepo.persist(ra);
                }
            }

            // 8. Ensure Control Admin user exists
            UserRepository userRepo = new UserRepository();
            Optional<User> adminOpt = userRepo.findByUsername(CONTROL_ADMIN_USERNAME);
            UUID adminUserId;
            if (adminOpt.isEmpty()) {
                adminUserId = UUID.randomUUID();
                String initialPassword = getControlAdminInitialPassword();
                String hashedPass = HashUtility.hash(initialPassword);
                User adminUser = new User(adminUserId, CONTROL_ADMIN_USERNAME, hashedPass);
                adminUser.setCreatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
                adminUser.setCreatedDate(LocalDateTime.now());
                userRepo.persist(adminUser);

                UserRoleRepository userRoleRepo = new UserRoleRepository();
                UserRoleEntity userRole = new UserRoleEntity(adminUserId, controlRole.getId());
                userRole.setCreatedBy(SystemHardcoding.SYSTEM_USER_ID.asUuid());
                userRole.setCreatedDate(LocalDateTime.now());
                userRoleRepo.persist(userRole);

                logger.info("[ControlPlaneBootstrap] Created default control admin user '{}'.", CONTROL_ADMIN_USERNAME);
            }

            // 9. Preload role authorizations cache for freelance
            new RoleAuthorizationService().preloadRoleAuthorizationCacheEntries();
        }

        // 10. Synchronize configurations and authorizations in the database for all active customer tenants
        for (TenantEntity tenant : allTenants) {
            if (controlTenant.equalsIgnoreCase(tenant.getTenantId())) {
                continue;
            }
            try (Transaction tenantTx = Transaction.start(tenant.getTenantId())) {
                Transaction.getRequestContext().withUserId(SystemHardcoding.SYSTEM_USER_ID.asUuid());

                new ConfigurationSeeder().seed();
                new AuthorizationSeeder().seed();

                logger.info("[ControlPlaneBootstrap] Synchronized configurations & authorizations for tenant '{}'.", tenant.getTenantId());
            } catch (Exception e) {
                logger.warn("[ControlPlaneBootstrap] Failed to sync tenant '{}': {}", tenant.getTenantId(), e.getMessage());
            }
        }

        logger.info("================================================================================");
        logger.info("[ControlPlaneBootstrap] Control Plane initialization complete.");
        logger.info("================================================================================");
    }
}
