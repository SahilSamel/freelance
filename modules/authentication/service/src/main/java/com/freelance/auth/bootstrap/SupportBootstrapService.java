package com.freelance.auth.bootstrap;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.Day0BootstrapResult;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.auth.vendor.VendorSupportKeyUtility;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.keys.authentication.support.SupportConfigKeys;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;
import com.freelance.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

public class SupportBootstrapService {

    private static final Logger logger = LoggerFactory.getLogger(SupportBootstrapService.class);

    private final UserRepository userRepository;
    private final SystemConfigurationService systemConfigurationService;
    private final SupportAccessService supportAccessService;

    public SupportBootstrapService() {
        this(new UserRepository(), new SystemConfigurationService(), new SupportAccessService());
    }

    public SupportBootstrapService(
            UserRepository userRepository,
            SystemConfigurationService systemConfigurationService,
            SupportAccessService supportAccessService
    ) {
        this.userRepository = userRepository;
        this.systemConfigurationService = systemConfigurationService;
        this.supportAccessService = supportAccessService;
    }

    public Day0BootstrapResult bootstrapIfRequired() {
        String activeTenant = SystemHardcoding.DEFAULT_TENANT.asString();
        try {
            if (Transaction.getRequestContext() != null && Transaction.getRequestContext().tenantId() != null) {
                activeTenant = Transaction.getRequestContext().tenantId();
            }
        } catch (Exception ignored) {
        }
        return bootstrapTenant(activeTenant);
    }

    public Day0BootstrapResult bootstrapTenant(String tenantId) {
        String schema = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim().toLowerCase() : SystemHardcoding.DEFAULT_TENANT.asString();
        String configuredPublicKey = ConfigurationEngine.get(SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY);
        boolean hasPublicKey = configuredPublicKey != null && !configuredPublicKey.isBlank();
        long userCount = userRepository.countUsers();

        if (hasPublicKey) {
            logger.debug("[SupportBootstrapService] Vendor support public key already configured for tenant '{}'. Skipping Day-0 bootstrap.", schema);
            return Day0BootstrapResult.skipped();
        }

        if (userCount > 0) {
            logger.warn("================================================================================");
            logger.warn("[SupportBootstrapService] WARNING: Vendor support public key is missing for tenant '{}',", schema);
            logger.warn("[SupportBootstrapService] but {} user(s) already exist in schema '{}'.", userCount, schema);
            logger.warn("[SupportBootstrapService] Skipping Day-0 bootstrap. Vendor support sessions will fail until a valid public key is configured.");
            logger.warn("================================================================================");
            return Day0BootstrapResult.skipped();
        }

        logger.info("[SupportBootstrapService] Fresh tenant installation detected for '{}' (0 users, unconfigured vendor key). Executing Day-0 Bootstrap Ceremony...", schema);

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();

            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            systemConfigurationService.updateConfiguration(new SystemConfigurationUpdateRequestDTO(
                    SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey(),
                    publicKeyBase64,
                    "RSA public key generated during Day-0 bootstrap for tenant: " + schema
            ));

            String privateKeyBase64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded());
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + privateKeyBase64 + "\n-----END PRIVATE KEY-----";

            Instant now = Instant.now();
            Instant expiresAt = now.plus(24, ChronoUnit.HOURS);

            supportAccessService.grantDay0Access(expiresAt);

            String day0Token = Jwts.builder()
                    .issuer(VendorSupportKeyUtility.VENDOR_ISSUER)
                    .subject(SystemHardcoding.VENDOR_SUPPORT_USER_ID.asString())
                    .claim("sessionKey", SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString())
                    .claim("tenantId", schema)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiresAt))
                    .signWith(keyPair.getPrivate())
                    .compact();

            logger.info("[SupportBootstrapService] Day-0 Support Ceremony successfully executed for tenant '{}' (expiresAt={}).", schema, expiresAt);

            return new Day0BootstrapResult(true, publicKeyBase64, privateKeyPem, day0Token, expiresAt);
        } catch (Exception e) {
            logger.error("[SupportBootstrapService] FATAL ERROR during Day-0 bootstrap ceremony for tenant '{}'!", schema, e);
            throw new IllegalStateException("Failed to execute Day-0 bootstrap ceremony for tenant: " + schema, e);
        }
    }
}
