package com.freelance.auth.service.constraint;

import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.fixtures.VendorSupportTestHelper;
import com.freelance.keys.authentication.AuthenticationConfigKeys;
import com.freelance.keys.authentication.ConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VerifyAuthConstraint Tests")
class VerifyAuthConstraintTest {

    private VerifyAuthConstraint constraint;
    private SupportAccessService supportAccessService;
    private KeyPair vendorKeyPair;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();

        supportAccessService = new SupportAccessService();
        constraint = new VerifyAuthConstraint(supportAccessService);
        vendorKeyPair = VendorSupportTestHelper.generateRsaKeyPair();

        // Configure vendor public key into cache
        String pubKeyBase64 = VendorSupportTestHelper.encodeKeyToBase64(vendorKeyPair.getPublic());
        CacheManager.getInstance().put("cache_SystemConfig",
                AuthenticationConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey(),
                pubKeyBase64);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("standard session token bypasses vendor support constraint checks")
    void standardToken_passes() {
        assertDoesNotThrow(() ->
                constraint.checkConstraints(new AuthVerifyRequestDTO("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.abc"))
        );
    }

    @Test
    @DisplayName("vendor token when public key is not configured raises INVALID_VENDOR_SUPPORT_TOKEN")
    void vendorToken_missingPublicKey_raisesError() {
        CacheManager.getInstance().invalidate("cache_SystemConfig", AuthenticationConfigKeys.VENDOR_SUPPORT_PUBLIC_KEY.getKey());

        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), "TICKET-1", "Reason", Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
        assertEquals(ConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("vendor token with invalid signature raises INVALID_VENDOR_SUPPORT_TOKEN")
    void vendorToken_invalidSignature_raisesError() {
        KeyPair anotherKeyPair = VendorSupportTestHelper.generateRsaKeyPair();
        String fakeToken = VendorSupportTestHelper.generateSupportToken(
                anotherKeyPair.getPrivate(), "TICKET-FAKE", "Malicious", Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(fakeToken))
        );
        assertEquals(ConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("vendor token when support access window is inactive raises SUPPORT_ACCESS_DISABLED")
    void vendorToken_windowInactive_raisesError() {
        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), "TICKET-1", "Reason", Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
        assertEquals(ConstraintErrors.SUPPORT_ACCESS_DISABLED.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("valid vendor token when support window is active passes constraints")
    void vendorToken_validAndActive_passes() {
        supportAccessService.grantAccess();

        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), "TICKET-1", "Reason", Duration.ofHours(1)
        );

        assertDoesNotThrow(() ->
                constraint.checkConstraints(new AuthVerifyRequestDTO(token))
        );
    }
}
