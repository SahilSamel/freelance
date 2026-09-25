package com.freelance.fixtures;

import com.freelance.auth.token.TokenUtility;
import io.jsonwebtoken.Jwts;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Test fixture helper providing mock RSA key generation and vendor token creation for tests.
 */
public final class VendorSupportTestHelper {

    private VendorSupportTestHelper() {
    }

    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not supported in this JVM", e);
        }
    }

    public static String encodeKeyToBase64(Key key) {
        if (key == null) return null;
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PrivateKey parsePrivateKey(String base64OrPem) {
        if (base64OrPem == null || base64OrPem.isBlank()) {
            throw new IllegalArgumentException("Private key string cannot be null or blank");
        }
        String sanitized = base64OrPem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        try {
            byte[] keyBytes = Base64.getDecoder().decode(sanitized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid RSA private key string", e);
        }
    }

    public static String generateSupportToken(PrivateKey privateKey, Duration duration) {
        if (privateKey == null) {
            throw new IllegalArgumentException("PrivateKey is required to generate vendor support token");
        }
        Instant now = Instant.now();
        Instant expiry = now.plus(duration != null ? duration : Duration.ofHours(1));

        return Jwts.builder()
                .issuer(TokenUtility.VENDOR_ISSUER)
                .subject(com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_USER_ID.asString())
                .claim("sessionKey", com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
