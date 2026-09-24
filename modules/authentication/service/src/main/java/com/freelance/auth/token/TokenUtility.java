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
}
