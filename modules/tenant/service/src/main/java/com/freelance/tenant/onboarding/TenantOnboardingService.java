package com.freelance.tenant.onboarding;

import com.freelance.auth.support.SupportAccessService;
import com.freelance.authorization.seeder.AuthorizationSeeder;
import com.freelance.config.DatabaseConfigHolder;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.error.ErrorConfigurationService;
import com.freelance.configuration.seeder.ConfigurationSeeder;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.flyway.FlywayMigrationRunner;
import com.freelance.keys.authentication.support.SupportConfigKeys;
import com.freelance.mapping.roleAuthorization.RoleAuthorizationService;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.jaxrs.tenant.TenantRegistry;
import com.freelance.system.SystemHardcoding;
import com.freelance.template.seeder.AlertsBootstrap;
import com.freelance.tenant.control.key.PlatformVendorKeyService;
import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.dto.TenantOnboardResponseDTO;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.onboarding.constraint.TenantOnboardConstraint;
import com.freelance.tenant.onboarding.validation.TenantOnboardValidation;
import com.freelance.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class TenantOnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(TenantOnboardingService.class);

    private final FlywayMigrationRunner migrationRunner;
    private final TenantRepository tenantRepository;
    private final PlatformVendorKeyService platformVendorKeyService;
    private final TenantOnboardValidation validation;
    private final TenantOnboardConstraint constraint;

    public TenantOnboardingService() {
        this(new FlywayMigrationRunner(), new TenantRepository(), new PlatformVendorKeyService());
    }

    public TenantOnboardingService(
            FlywayMigrationRunner migrationRunner,
            TenantRepository tenantRepository,
            PlatformVendorKeyService platformVendorKeyService
    ) {
        this(migrationRunner, tenantRepository, platformVendorKeyService, new TenantOnboardValidation(), new TenantOnboardConstraint(tenantRepository));
    }

    public TenantOnboardingService(
            FlywayMigrationRunner migrationRunner,
            TenantRepository tenantRepository,
            PlatformVendorKeyService platformVendorKeyService,
            TenantOnboardValidation validation,
            TenantOnboardConstraint constraint
    ) {
        this.migrationRunner = Objects.requireNonNull(migrationRunner, "migrationRunner must not be null");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
        this.platformVendorKeyService = Objects.requireNonNull(platformVendorKeyService, "platformVendorKeyService must not be null");
        this.validation = validation != null ? validation : new TenantOnboardValidation();
        this.constraint = constraint != null ? constraint : new TenantOnboardConstraint(this.tenantRepository);
    }

    public TenantOnboardResponseDTO onboardTenant(TenantCreateRequestDTO request) {
        String controlTenant = SystemHardcoding.DEFAULT_TENANT.asString();
        String tenantId = (request != null && request.tenantId() != null) ? request.tenantId().trim().toLowerCase() : null;
        String name = (request != null && request.name() != null && !request.name().isBlank()) ? request.name().trim() : tenantId;

        boolean managedControlTx = !Transaction.hasActiveTransaction();
        if (managedControlTx) {
            Transaction.start(controlTenant);
        }

        try {
            // Stage 1: Control Tenant - Validation, Constraint Checks & Vendor Public Key Resolution
            validation.validateRequest(request);
            constraint.checkConstraints(request);

            String effectivePublicKey = (request.publicKey() != null && !request.publicKey().isBlank())
                    ? request.publicKey().trim()
                    : platformVendorKeyService.getPlatformPublicKeyBase64();

            logger.info("[TenantOnboardingService] Provisioning tenant '{}' ({})...", tenantId, name);

            // Run Flyway DDL migrations to create schema and tables
            migrationRunner.migrate(DatabaseConfigHolder.get(), tenantId);

            // Stage 2: New Tenant - Seed Domain Data, Preload Caches, Inject Public Key & Grant Day-0 Support
            Instant day0ExpiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
            TransactionContext outerContext = Transaction.hasActiveTransaction() ? Transaction.peek() : null;
            try (Transaction tx = Transaction.start(tenantId)) {
                Transaction.getRequestContext().withUserId(SystemHardcoding.SYSTEM_USER_ID.asUuid());

                new ConfigurationSeeder().seed();
                new AuthorizationSeeder().seed();

                SystemConfigurationService systemConfigurationService = new SystemConfigurationService();
                systemConfigurationService.preloadSystemCacheEntries();
                new ErrorConfigurationService().preloadErrorCacheEntries();
                new RoleAuthorizationService().preloadRoleAuthorizationCacheEntries();
                AlertsBootstrap.init();

                if (effectivePublicKey != null && !effectivePublicKey.isBlank()) {
                    systemConfigurationService.updateConfiguration(new SystemConfigurationUpdateRequestDTO(
                            SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey(),
                            effectivePublicKey,
                            "Global Platform Vendor RSA Public Key for tenant: " + tenantId
                    ));
                }

                new SupportAccessService().grantDay0Access(day0ExpiresAt);
            } finally {
                if (outerContext != null) {
                    Transaction.bind(outerContext);
                }
            }

            // Stage 3: Control Tenant - Mint Day-0 Token, Persist in FL_TENANTS & Register Cache
            String day0Token = platformVendorKeyService.mintVendorToken(tenantId, day0ExpiresAt);
            tenantRepository.persist(new TenantEntity(tenantId, name, TenantRegistry.STATUS_ACTIVE));
            TenantRegistry.registerTenant(tenantId, TenantRegistry.STATUS_ACTIVE);

            logger.info("[TenantOnboardingService] Tenant '{}' successfully provisioned and registered!", tenantId);
            return new TenantOnboardResponseDTO(tenantId, name, TenantRegistry.STATUS_ACTIVE, effectivePublicKey, day0Token, day0ExpiresAt);
        } finally {
            if (managedControlTx && Transaction.hasActiveTransaction()) {
                Transaction.finish();
            }
        }
    }
}
