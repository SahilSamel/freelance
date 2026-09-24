package com.freelance.auth.service.constraint;

import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.auth.dto.TokenGenerateRequestDTO;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.auth.token.TokenUtility;
import com.freelance.auth.vendor.VendorSupportKeyUtility;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.fixtures.VendorSupportTestHelper;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.keys.authentication.support.SupportConfigKeys;
import com.freelance.keys.authentication.support.SupportConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.Transaction;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VerifyAuthConstraint Tests")
class VerifyAuthConstraintTest {

    private VerifyAuthConstraint constraint;
    private SupportAccessService supportAccessService;
    private TokenUtility tokenUtility;
    private KeyPair vendorKeyPair;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();

        supportAccessService = new SupportAccessService();
        tokenUtility = new TokenUtility();
        constraint = new VerifyAuthConstraint(supportAccessService, tokenUtility);
        vendorKeyPair = VendorSupportTestHelper.generateRsaKeyPair();

        // Configure vendor public key into cache
        String pubKeyBase64 = VendorSupportTestHelper.encodeKeyToBase64(vendorKeyPair.getPublic());
        CacheManager.put("cache_SystemConfig",
                SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey(),
                pubKeyBase64);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("standard session token bypasses vendor support constraint checks")
    void standardToken_passes() {
        String token = tokenUtility.generateToken(new TokenGenerateRequestDTO(UUID.randomUUID(), "sess-123", "freelance")).token();
        assertDoesNotThrow(() ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
    }

    @Test
    @DisplayName("standard token with matching tenant passes constraint check")
    void standardToken_sameTenant_passes() {
        Transaction.getRequestContext().withTenantId("ironfit");
        String token = tokenUtility.generateToken(new TokenGenerateRequestDTO(UUID.randomUUID(), "sess-123", "ironfit")).token();

        assertDoesNotThrow(() ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
    }

    @Test
    @DisplayName("standard token with mismatched tenant raises INVALID_SESSION constraint error")
    void standardToken_crossTenant_raisesError() {
        Transaction.getRequestContext().withTenantId("crossgym");
        String token = tokenUtility.generateToken(new TokenGenerateRequestDTO(UUID.randomUUID(), "sess-123", "ironfit")).token();

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
        assertEquals(SessionConstraintErrors.INVALID_SESSION.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("vendor token when public key is not configured raises INVALID_VENDOR_SUPPORT_TOKEN")
    void vendorToken_missingPublicKey_raisesError() {
        CacheManager.invalidate("cache_SystemConfig", SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey());

        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
        assertEquals(SupportConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("vendor token with invalid signature raises INVALID_VENDOR_SUPPORT_TOKEN")
    void vendorToken_invalidSignature_raisesError() {
        KeyPair anotherKeyPair = VendorSupportTestHelper.generateRsaKeyPair();
        String fakeToken = VendorSupportTestHelper.generateSupportToken(
                anotherKeyPair.getPrivate(), Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(fakeToken))
        );
        assertEquals(SupportConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("vendor token when support access window is inactive raises SUPPORT_ACCESS_DISABLED")
    void vendorToken_windowInactive_raisesError() {
        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
        assertEquals(SupportConstraintErrors.SUPPORT_ACCESS_DISABLED.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("valid vendor token when support window is active passes constraints")
    void vendorToken_validAndActive_passes() {
        supportAccessService.grantAccess();

        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), Duration.ofHours(1)
        );

        assertDoesNotThrow(() ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
    }

    @Test
    @DisplayName("vendor token with mismatched tenant claim raises INVALID_SESSION")
    void vendorToken_crossTenant_raisesError() {
        Transaction.getRequestContext().withTenantId("crossgym");
        String pubKeyBase64 = VendorSupportTestHelper.encodeKeyToBase64(vendorKeyPair.getPublic());
        CacheManager.put("cache_SystemConfig",
                SupportConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey(),
                pubKeyBase64);
        supportAccessService.grantAccess();

        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer(VendorSupportKeyUtility.VENDOR_ISSUER)
                .subject(UUID.randomUUID().toString())
                .claim("sessionKey", "sess-vendor")
                .claim("tenantId", "ironfit")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(vendorKeyPair.getPrivate())
                .compact();

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
        assertEquals(SessionConstraintErrors.INVALID_SESSION.getIndirectionCode(), ex.getError().getIndirectionCode());
    }
}
