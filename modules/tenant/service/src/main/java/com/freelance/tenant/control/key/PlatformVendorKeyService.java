package com.freelance.tenant.control.key;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.token.TokenUtility;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.keys.control.ControlPlaneConfigKeys;
import com.freelance.system.SystemHardcoding;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class PlatformVendorKeyService {

    private static final Logger logger = LoggerFactory.getLogger(PlatformVendorKeyService.class);
    private final SystemConfigurationService configService;

    public PlatformVendorKeyService() {
        this(new SystemConfigurationService());
    }

    public PlatformVendorKeyService(SystemConfigurationService configService) {
        this.configService = configService;
    }

    public void ensureKeysExist() {
        String pubKey = ConfigurationEngine.get(ControlPlaneConfigKeys.PLATFORM_VENDOR_PUBLIC_KEY);
        String privKey = ConfigurationEngine.get(ControlPlaneConfigKeys.PLATFORM_VENDOR_PRIVATE_KEY);

        if (pubKey != null && !pubKey.isBlank() && privKey != null && !privKey.isBlank()) {
            logger.debug("[PlatformVendorKeyService] Platform vendor RSA keypair already established.");
            return;
        }

        try {
            logger.info("[PlatformVendorKeyService] Generating new master 2048-bit RSA keypair for Control Plane...");
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();

            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKeyBase64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded());
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + privateKeyBase64 + "\n-----END PRIVATE KEY-----";

            configService.updateConfiguration(new SystemConfigurationUpdateRequestDTO(
                    ControlPlaneConfigKeys.PLATFORM_VENDOR_PUBLIC_KEY.getKey(),
                    publicKeyBase64,
                    "Global Platform Vendor RSA Public Key"
            ));

            configService.updateConfiguration(new SystemConfigurationUpdateRequestDTO(
                    ControlPlaneConfigKeys.PLATFORM_VENDOR_PRIVATE_KEY.getKey(),
                    privateKeyPem,
                    "Global Platform Vendor RSA Private Key"
            ));

            logger.info("[PlatformVendorKeyService] Master Platform Vendor RSA keypair generated and stored successfully.");
        } catch (Exception e) {
            logger.error("[PlatformVendorKeyService] Failed to generate/store Platform Vendor RSA keys", e);
            throw new IllegalStateException("Failed to initialize Platform Vendor RSA keys", e);
        }
    }

    public String getPlatformPublicKeyBase64() {
        return ConfigurationEngine.get(ControlPlaneConfigKeys.PLATFORM_VENDOR_PUBLIC_KEY);
    }

    public PrivateKey getPlatformPrivateKey() {
        String pem = ConfigurationEngine.get(ControlPlaneConfigKeys.PLATFORM_VENDOR_PRIVATE_KEY);
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("Platform Vendor Private Key is not initialized in Control Plane configuration!");
        }

        try {
            String base64Key = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode Platform Vendor Private Key", e);
        }
    }

    public String mintVendorToken(String tenantId, Instant expiresAt) {
        PrivateKey privateKey = getPlatformPrivateKey();
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(TokenUtility.VENDOR_ISSUER)
                .subject(SystemHardcoding.VENDOR_SUPPORT_USER_ID.asString())
                .claim("sessionKey", SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString())
                .claim("tenantId", tenantId.trim().toLowerCase())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(privateKey)
                .compact();
    }
}
