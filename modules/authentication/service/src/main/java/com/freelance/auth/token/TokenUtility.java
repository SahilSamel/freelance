package com.freelance.auth.token;

import com.freelance.auth.dto.TokenGenerateRequestDTO;
import com.freelance.auth.dto.TokenGenerateResponseDTO;
import com.freelance.auth.dto.TokenParseRequestDTO;
import com.freelance.auth.dto.TokenParseResponseDTO;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class TokenUtility {

    private static final String CLAIM_SESSION_KEY = "sessionKey";
    private static final String CLAIM_TENANT_ID = "tenantId";
    private static final Logger logger = LoggerFactory.getLogger(TokenUtility.class);

    public TokenGenerateResponseDTO generateToken(TokenGenerateRequestDTO request) {
        if (request == null || request.userId() == null || request.sessionKey() == null) {
            throw new IllegalArgumentException("UserId and sessionKey are required to generate token.");
        }

        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(request.userId().toString())
                .claim(CLAIM_SESSION_KEY, request.sessionKey())
                .issuedAt(Date.from(now));

        if (request.tenantId() != null && !request.tenantId().isBlank()) {
            String tenant = request.tenantId().trim().toLowerCase();
            builder.issuer(tenant);
            builder.claim(CLAIM_TENANT_ID, tenant);
        }

        String token = builder
                .signWith(signingKey())
                .compact();

        return new TokenGenerateResponseDTO(token);
    }

    public TokenParseResponseDTO parse(TokenParseRequestDTO request) {

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(request.token())
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String sessionKey = claims.get(CLAIM_SESSION_KEY, String.class);
            String tenantId = claims.getIssuer() != null && !claims.getIssuer().isBlank()
                    ? claims.getIssuer()
                    : claims.get(CLAIM_TENANT_ID, String.class);

            return new TokenParseResponseDTO(userId, sessionKey, tenantId);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            throw new ConstraintException(SessionConstraintErrors.EXPIRED_SESSION);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Failed to parse JWT token: {}", e.getMessage());
            throw new ConstraintException(SessionConstraintErrors.INVALID_SESSION);
        }
    }

    private SecretKey signingKey() {
        return JwtSecretManager.getSecretKey();
    }

    public static final String VENDOR_ISSUER = "vendor-support";

    /**
     * Inspects the unverified JWT payload claims to determine if the token was issued for vendor technical support.
     *
     * @param token the JWT string
     * @return true if the token contains the vendor-support issuer
     */
    public boolean isVendorToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payloadBytes, java.nio.charset.StandardCharsets.UTF_8);
            return payloadJson.contains("\"" + VENDOR_ISSUER + "\"") || payloadJson.contains(VENDOR_ISSUER);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses an RSA public key from either a Base64-encoded X.509 string or a standard PEM-formatted string.
     *
     * @param base64OrPem the raw public key string
     * @return the parsed PublicKey instance
     */
    public java.security.PublicKey parsePublicKey(String base64OrPem) {
        if (base64OrPem == null || base64OrPem.isBlank()) {
            throw new IllegalArgumentException("Public key string cannot be null or blank");
        }
        String sanitized = sanitizeKeyString(base64OrPem);
        try {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(sanitized);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            logger.error("Failed to parse RSA public key from string", e);
            throw new IllegalArgumentException("Invalid RSA public key string", e);
        }
    }

    /**
     * Verifies and parses an asymmetric RSA vendor support token using the tenant's configured RSA public key.
     *
     * @param token     the JWT string
     * @param publicKey the tenant's RSA public key
     * @return the parsed token response DTO
     */
    public TokenParseResponseDTO parseVendorToken(String token, java.security.PublicKey publicKey) {
        Claims claims = verifyAndParseSupportToken(token, publicKey);
        UUID userId = UUID.fromString(claims.getSubject());
        String sessionKey = claims.get(CLAIM_SESSION_KEY, String.class);
        String tenantId = claims.get(CLAIM_TENANT_ID, String.class);
        return new TokenParseResponseDTO(userId, sessionKey, tenantId);
    }

    /**
     * Verifies and extracts raw Claims from an RSA vendor support token using the provided public key.
     *
     * @param token     the JWT string
     * @param publicKey the tenant's RSA public key
     * @return the verified JWT Claims
     */
    public Claims verifyAndParseSupportToken(String token, java.security.PublicKey publicKey) {
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
