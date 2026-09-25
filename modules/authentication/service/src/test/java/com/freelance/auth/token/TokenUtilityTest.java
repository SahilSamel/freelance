package com.freelance.auth.token;

import com.freelance.auth.dto.TokenGenerateRequestDTO;
import com.freelance.auth.dto.TokenGenerateResponseDTO;
import com.freelance.auth.dto.TokenParseRequestDTO;
import com.freelance.auth.dto.TokenParseResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilityTest {

    private TokenUtility tokenUtility;

    @BeforeEach
    void setUp() {
        tokenUtility = new TokenUtility();
        JwtSecretManager.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        JwtSecretManager.resetForTesting();
    }

    @Test
    @DisplayName("generateToken and parse succeed with ephemeral in-memory secret")
    void generateAndParse_succeedWithEphemeralSecret() {
        UUID userId = UUID.randomUUID();
        String sessionKey = "sample-session-key";
        TokenGenerateRequestDTO request = new TokenGenerateRequestDTO(userId, sessionKey);

        TokenGenerateResponseDTO generateResponse = tokenUtility.generateToken(request);
        assertNotNull(generateResponse);
        assertNotNull(generateResponse.token());

        TokenParseResponseDTO parseResponse = tokenUtility.parse(new TokenParseRequestDTO(generateResponse.token()));
        assertNotNull(parseResponse);
        assertEquals(userId, parseResponse.userId());
        assertEquals(sessionKey, parseResponse.sessionKey());
    }

    @Test
    @DisplayName("parse throws ConstraintException when token is malformed")
    void parse_throwsWhenTokenIsMalformed() {
        TokenParseRequestDTO request = new TokenParseRequestDTO("some.invalid.jwt.token");

        assertThrows(
                com.freelance.rest.app.service.exception.constraint.ConstraintException.class,
                () -> tokenUtility.parse(request)
        );
    }

    @Test
    @DisplayName("generateToken and parse succeed when custom test secret is supplied")
    void generateAndParse_succeedWithCustomSecret() {
        String customSecret = "custom-super-secure-production-jwt-secret-key-123456789!";
        JwtSecretManager.setSecretForTesting(customSecret);

        UUID userId = UUID.randomUUID();
        String sessionKey = "session-12345";

        TokenGenerateResponseDTO generateResponse = tokenUtility.generateToken(
                new TokenGenerateRequestDTO(userId, sessionKey)
        );

        assertNotNull(generateResponse);
        assertNotNull(generateResponse.token());
        assertFalse(generateResponse.token().isBlank());

        TokenParseResponseDTO parseResponse = tokenUtility.parse(
                new TokenParseRequestDTO(generateResponse.token())
        );

        assertNotNull(parseResponse);
        assertEquals(userId, parseResponse.userId());
        assertEquals(sessionKey, parseResponse.sessionKey());
    }

    @Test
    @DisplayName("generateToken throws IllegalArgumentException when request or fields are null")
    void generateToken_nullGuards() {
        assertThrows(IllegalArgumentException.class, () -> tokenUtility.generateToken(null));
        assertThrows(IllegalArgumentException.class, () -> tokenUtility.generateToken(new TokenGenerateRequestDTO(null, "key")));
        assertThrows(IllegalArgumentException.class, () -> tokenUtility.generateToken(new TokenGenerateRequestDTO(UUID.randomUUID(), null)));
    }

    @Test
    @DisplayName("parse throws ConstraintException EXPIRED_SESSION when token is expired")
    void parse_expiredToken() {
        UUID userId = UUID.randomUUID();
        String sessionKey = "expired-session";

        // Build an expired token manually
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .subject(userId.toString())
                .claim("sessionKey", sessionKey)
                .issuedAt(java.util.Date.from(java.time.Instant.now().minusSeconds(3600)))
                .expiration(java.util.Date.from(java.time.Instant.now().minusSeconds(1800)))
                .signWith(JwtSecretManager.getSecretKey())
                .compact();

        com.freelance.rest.app.service.exception.constraint.ConstraintException ex = assertThrows(
                com.freelance.rest.app.service.exception.constraint.ConstraintException.class,
                () -> tokenUtility.parse(new TokenParseRequestDTO(expiredToken))
        );
        assertEquals(com.freelance.keys.authentication.session.SessionConstraintErrors.EXPIRED_SESSION, ex.getError());
    }

    @Test
    @DisplayName("generateToken and parse correctly propagate tenantId claim and standard issuer claim")
    void generateAndParse_withTenantId() {
        UUID userId = UUID.randomUUID();
        String sessionKey = "sess-tenant-123";
        String tenantId = "ironfit";

        TokenGenerateRequestDTO request = new TokenGenerateRequestDTO(userId, sessionKey, tenantId);
        TokenGenerateResponseDTO generateResponse = tokenUtility.generateToken(request);
        assertNotNull(generateResponse.token());

        // Verify standard claims
        io.jsonwebtoken.Claims rawClaims = io.jsonwebtoken.Jwts.parser()
                .verifyWith(JwtSecretManager.getSecretKey())
                .build()
                .parseSignedClaims(generateResponse.token())
                .getPayload();
        assertEquals("ironfit", rawClaims.getIssuer());
        assertEquals("ironfit", rawClaims.get("tenantId", String.class));

        TokenParseResponseDTO parseResponse = tokenUtility.parse(new TokenParseRequestDTO(generateResponse.token()));
        assertEquals(userId, parseResponse.userId());
        assertEquals(sessionKey, parseResponse.sessionKey());
        assertEquals("ironfit", parseResponse.tenantId());
    }

    @Test
    @DisplayName("isVendorToken, parsePublicKey, and parseVendorToken work correctly for RSA tokens")
    void testVendorTokenOperations() throws Exception {
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair keyPair = kpg.generateKeyPair();

        String pubBase64 = java.util.Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        java.security.PublicKey parsedPub = tokenUtility.parsePublicKey(pubBase64);
        assertNotNull(parsedPub);

        UUID vendorUserId = UUID.randomUUID();
        String sessionKey = "vendor-session";
        String tenantId = "ironfit";

        String vendorToken = io.jsonwebtoken.Jwts.builder()
                .issuer(TokenUtility.VENDOR_ISSUER)
                .subject(vendorUserId.toString())
                .claim("sessionKey", sessionKey)
                .claim("tenantId", tenantId)
                .issuedAt(new java.util.Date())
                .signWith(keyPair.getPrivate())
                .compact();

        assertTrue(tokenUtility.isVendorToken(vendorToken));
        assertFalse(tokenUtility.isVendorToken("not.a.token"));
        assertFalse(tokenUtility.isVendorToken(null));

        TokenParseResponseDTO parsedVendor = tokenUtility.parseVendorToken(vendorToken, parsedPub);
        assertEquals(vendorUserId, parsedVendor.userId());
        assertEquals(sessionKey, parsedVendor.sessionKey());
        assertEquals(tenantId, parsedVendor.tenantId());
    }
}
