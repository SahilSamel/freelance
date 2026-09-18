package com.freelance.auth.vendor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class VendorSupportKeyUtility {

    private static final Logger logger = LoggerFactory.getLogger(VendorSupportKeyUtility.class);
    public static final String VENDOR_ISSUER = "vendor-support";

    private VendorSupportKeyUtility() {
    }

    public static boolean isVendorToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            return payloadJson.contains("\"" + VENDOR_ISSUER + "\"") || payloadJson.contains(VENDOR_ISSUER);
        } catch (Exception e) {
            return false;
        }
    }

    public static PublicKey parsePublicKey(String base64OrPem) {
        if (base64OrPem == null || base64OrPem.isBlank()) {
            throw new IllegalArgumentException("Public key string cannot be null or blank");
        }
        String sanitized = sanitizeKeyString(base64OrPem);
        try {
            byte[] keyBytes = Base64.getDecoder().decode(sanitized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            logger.error("Failed to parse RSA public key from string", e);
            throw new IllegalArgumentException("Invalid RSA public key string", e);
        }
    }

    public static Claims verifyAndParseSupportToken(String token, PublicKey publicKey) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        if (publicKey == null) {
            throw new IllegalStateException("Vendor public key is not configured");
        }
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Vendor support token verification failed: {}", e.getMessage());
            throw e;
        }
    }

    private static String sanitizeKeyString(String key) {
        return key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
