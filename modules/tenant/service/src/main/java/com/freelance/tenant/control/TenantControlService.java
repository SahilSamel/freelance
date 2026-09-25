package com.freelance.tenant.control;

import com.freelance.rest.jaxrs.tenant.TenantRegistry;
import com.freelance.tenant.control.constraint.TenantDeactivateConstraint;
import com.freelance.tenant.control.constraint.TenantSupportSessionConstraint;
import com.freelance.tenant.control.key.PlatformVendorKeyService;
import com.freelance.tenant.control.validation.TenantDeactivateValidation;
import com.freelance.tenant.dto.SupportSessionResponseDTO;
import com.freelance.tenant.dto.TenantListResponseDTO;
import com.freelance.tenant.dto.TenantResponseDTO;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class TenantControlService {

    private static final Logger logger = LoggerFactory.getLogger(TenantControlService.class);

    private final TenantRepository tenantRepository;
    private final PlatformVendorKeyService platformVendorKeyService;
    private final TenantDeactivateValidation deactivateValidation;
    private final TenantDeactivateConstraint deactivateConstraint;
    private final TenantSupportSessionConstraint supportSessionConstraint;

    public TenantControlService() {
        this(new TenantRepository(), new PlatformVendorKeyService());
    }

    public TenantControlService(TenantRepository tenantRepository, PlatformVendorKeyService platformVendorKeyService) {
        this(
                tenantRepository,
                platformVendorKeyService,
                new TenantDeactivateValidation(),
                new TenantDeactivateConstraint(tenantRepository),
                new TenantSupportSessionConstraint(tenantRepository)
        );
    }

    public TenantControlService(
            TenantRepository tenantRepository,
            PlatformVendorKeyService platformVendorKeyService,
            TenantDeactivateValidation deactivateValidation,
            TenantDeactivateConstraint deactivateConstraint,
            TenantSupportSessionConstraint supportSessionConstraint
    ) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
        this.platformVendorKeyService = Objects.requireNonNull(platformVendorKeyService, "platformVendorKeyService must not be null");
        this.deactivateValidation = deactivateValidation != null ? deactivateValidation : new TenantDeactivateValidation();
        this.deactivateConstraint = deactivateConstraint != null ? deactivateConstraint : new TenantDeactivateConstraint(this.tenantRepository);
        this.supportSessionConstraint = supportSessionConstraint != null ? supportSessionConstraint : new TenantSupportSessionConstraint(this.tenantRepository);
    }

    public TenantListResponseDTO listTenants() {
        List<TenantEntity> entities = tenantRepository.findAllTenants();
        List<TenantResponseDTO> dtos = entities.stream()
                .map(e -> new TenantResponseDTO(e.getTenantId(), e.getName(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt()))
                .toList();

        return new TenantListResponseDTO(dtos);
    }

    public TenantResponseDTO deactivateTenant(String rawTenantId) {
        deactivateValidation.validateRequest(rawTenantId);

        String tenantId = rawTenantId.trim().toLowerCase();
        TenantEntity entity = deactivateConstraint.checkConstraints(tenantId);

        entity.setStatus(TenantRegistry.STATUS_DEACTIVATED);
        tenantRepository.merge(entity);

        // Update fast in-memory status map
        TenantRegistry.setStatus(tenantId, TenantRegistry.STATUS_DEACTIVATED);

        logger.info("[TenantControlService] Tenant '{}' has been soft-deleted (status=DEACTIVATED).", tenantId);
        return new TenantResponseDTO(entity.getTenantId(), entity.getName(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public SupportSessionResponseDTO generateSupportSession(String rawTenantId) {
        deactivateValidation.validateRequest(rawTenantId);

        String tenantId = rawTenantId.trim().toLowerCase();
        supportSessionConstraint.checkConstraints(tenantId);

        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        String vendorToken = platformVendorKeyService.mintVendorToken(tenantId, expiresAt);

        logger.info("[TenantControlService] Ephemeral vendor support session minted for tenant '{}' (expiresAt={})", tenantId, expiresAt);
        return new SupportSessionResponseDTO(tenantId, vendorToken, expiresAt);
    }
}
