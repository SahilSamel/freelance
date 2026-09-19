package com.freelance.auth.bootstrap;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.Day0BootstrapResult;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.auth.vendor.VendorSupportKeyUtility;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.keys.authentication.support.SupportConfigKeys;
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
        String configuredPublicKey = ConfigurationEngine.get(SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY);
        boolean hasPublicKey = configuredPublicKey != null && !configuredPublicKey.isBlank();
        long userCount = userRepository.countUsers();

        if (hasPublicKey) {
            logger.debug("[SupportBootstrapService] Vendor support public key is already configured. Skipping Day-0 bootstrap.");
            return Day0BootstrapResult.skipped();
        }

        if (userCount > 0) {
            logger.warn("================================================================================");
            logger.warn("[SupportBootstrapService] WARNING: Vendor support public key is unconfigured/missing,");
            logger.warn("[SupportBootstrapService] but {} user(s) already exist in the database.", userCount);
            logger.warn("[SupportBootstrapService] Skipping Day-0 bootstrap. Vendor support sessions will fail until a valid public key is configured.");
            logger.warn("================================================================================");
            return Day0BootstrapResult.skipped();
        }

        logger.info("[SupportBootstrapService] Fresh system installation detected (0 users, unconfigured vendor public key). Executing Day-0 Bootstrap Ceremony...");

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();

            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            systemConfigurationService.updateConfiguration(new SystemConfigurationUpdateRequestDTO(
                    SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey(),
                    publicKeyBase64,
                    "RSA public key generated during Day-0 bootstrap"
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
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiresAt))
                    .signWith(keyPair.getPrivate())
                    .compact();

            try {
                java.nio.file.Path tokenPath = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "freelance-day0-token.jwt");
                java.nio.file.Files.writeString(tokenPath, day0Token);
                java.nio.file.Path keyPath = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "freelance-day0-key.pem");
                java.nio.file.Files.writeString(keyPath, privateKeyPem);
            } catch (Exception ignored) {}

            printBootstrapBanner(privateKeyPem, day0Token);

            return new Day0BootstrapResult(true, publicKeyBase64, privateKeyPem, day0Token, expiresAt);
        } catch (Exception e) {
            logger.error("[SupportBootstrapService] FATAL ERROR during Day-0 bootstrap ceremony!", e);
            throw new IllegalStateException("Failed to execute Day-0 bootstrap ceremony", e);
        }
    }

    private void printBootstrapBanner(String privateKeyPem, String token) {
        StringBuilder banner = new StringBuilder();
        banner.append("\n================================================================================\n");
        banner.append("========================= DAY-0 SYSTEM BOOTSTRAP CEREMONY =======================\n");
        banner.append("================================================================================\n");
        banner.append("NOTICE: Fresh installation detected. An RSA 2048-bit key pair has been generated,\n");
        banner.append("the public key saved to FL_SYSTEM_CONFIGURATION, and a 24-hour support access\n");
        banner.append("window activated.\n\n");
        banner.append(">>> SAVE THIS PRIVATE KEY SECURELY. IT WILL NOT BE STORED OR DISPLAYED AGAIN! <<<\n\n");
        banner.append("RSA PRIVATE KEY (PKCS#8 PEM):\n");
        banner.append("--------------------------------------------------------------------------------\n");
        banner.append(privateKeyPem).append("\n");
        banner.append("--------------------------------------------------------------------------------\n\n");
        banner.append("DAY-0 VENDOR SUPPORT TOKEN (Expires in 24 hours):\n");
        banner.append("--------------------------------------------------------------------------------\n");
        banner.append("Bearer ").append(token).append("\n");
        banner.append("--------------------------------------------------------------------------------\n");
        banner.append("================================================================================\n");

        System.out.println(banner);
        logger.info("Day-0 System Bootstrap Ceremony executed successfully.");
    }
}
