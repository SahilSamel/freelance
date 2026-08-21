package com.freelance.auth.token;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.TokenGenerateRequestDTO;
import com.freelance.auth.dto.TokenGenerateResponseDTO;
import com.freelance.auth.dto.TokenParseRequestDTO;
import com.freelance.auth.dto.TokenParseResponseDTO;
import com.freelance.keys.authentication.AuthenticationConfigKeys;
import com.freelance.rest.app.service.exception.config.ConfigException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Owns the JWT logic for the Authentication module.
 */
public class TokenUtility {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String CLAIM_SESSION_KEY = "sessionKey";
    private static final Logger logger = LoggerFactory.getLogger(TokenUtility.class);

    public TokenGenerateResponseDTO generateToken(TokenGenerateRequestDTO request) {
        if (request == null || request.userId() == null || request.sessionKey() == null) {
            throw new IllegalArgumentException("UserId and sessionKey are required to generate token.");
        }

        long ttlSeconds = ConfigurationEngine.getLong(AuthenticationConfigKeys.SESSION_TTL_SECONDS);

        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(request.userId().toString())
                .claim(CLAIM_SESSION_KEY, request.sessionKey())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
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

            return new TokenParseResponseDTO(userId, sessionKey);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Failed to parse JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired token");
        }
    }

    private SecretKey signingKey() {
        String secret = ConfigurationEngine.get(AuthenticationConfigKeys.JWT_SECRET_KEY);
        if (secret == null || secret.isBlank()) {
            throw new ConfigException(AuthenticationConfigKeys.JWT_SECRET_KEY.getKey());
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT signing secret must be at least " + MIN_SECRET_BYTES + " bytes.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
