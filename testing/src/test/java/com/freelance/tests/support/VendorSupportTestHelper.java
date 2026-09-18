package com.freelance.tests.support;

import com.freelance.auth.vendor.VendorSupportKeyUtility;
import io.jsonwebtoken.Jwts;

import java.security.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Test fixture helper providing mock RSA key generation and vendor token creation for integration tests.
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

    public static String generateSupportToken(PrivateKey privateKey, Duration duration) {
        if (privateKey == null) {
            throw new IllegalArgumentException("PrivateKey is required to generate vendor support token");
        }
        Instant now = Instant.now();
        Instant expiry = now.plus(duration != null ? duration : Duration.ofHours(1));

        return Jwts.builder()
                .issuer(VendorSupportKeyUtility.VENDOR_ISSUER)
                .subject(com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_USER_ID.asString())
                .claim("sessionKey", com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
