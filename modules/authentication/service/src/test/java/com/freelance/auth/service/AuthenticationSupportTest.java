package com.freelance.auth.service;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.AuthVerifyRequestDTO;
import com.freelance.auth.dto.AuthVerifyResponseDTO;
import com.freelance.auth.support.SupportAccessService;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.fixtures.VendorSupportTestHelper;
import com.freelance.keys.authentication.AuthenticationConfigKeys;
import com.freelance.keys.authentication.ConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.system.SystemHardcoding;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationSupportTest {

    private Authentication authentication;
    private SupportAccessService supportAccessService;
    private KeyPair vendorKeyPair;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();

        authentication = new Authentication();
        supportAccessService = new SupportAccessService();
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
    @DisplayName("verify throws SUPPORT_ACCESS_DISABLED when support window is inactive")
    void testVerify_supportWindowClosed_throwsConstraintException() {
        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), "TICKET-101", "Debug DB lock", Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                authentication.verify(new AuthVerifyRequestDTO(token))
        );
        assertEquals(ConstraintErrors.SUPPORT_ACCESS_DISABLED.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("verify succeeds and returns SYSTEM_USER_ID and VENDOR_SUPPORT_SESSION when window is active")
    void testVerify_supportWindowActive_succeeds() {
        // Grant support access
        supportAccessService.grantAccess();

        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), "TICKET-101", "Debug DB lock", Duration.ofHours(1)
        );

        AuthVerifyResponseDTO response = authentication.verify(new AuthVerifyRequestDTO(token));
        assertNotNull(response);
        assertEquals(SystemHardcoding.VENDOR_SUPPORT_USER_ID.asUuid(), response.userId());
        assertEquals(SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString(), response.sessionKey());
    }

    @Test
    @DisplayName("verify throws INVALID_VENDOR_SUPPORT_TOKEN when token signature is invalid")
    void testVerify_invalidSignature_throwsConstraintException() {
        supportAccessService.grantAccess();

        KeyPair anotherKeyPair = VendorSupportTestHelper.generateRsaKeyPair();
        String fakeToken = VendorSupportTestHelper.generateSupportToken(
                anotherKeyPair.getPrivate(), "TICKET-FAKE", "Malicious attempt", Duration.ofHours(1)
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                authentication.verify(new AuthVerifyRequestDTO(fakeToken))
        );
        assertEquals(ConstraintErrors.INVALID_VENDOR_SUPPORT_TOKEN.getIndirectionCode(), ex.getError().getIndirectionCode());
    }

    @Test
    @DisplayName("verify throws SUPPORT_ACCESS_DISABLED immediately after access is revoked")
    void testVerify_afterRevocation_throwsConstraintException() {
        supportAccessService.grantAccess();
        String token = VendorSupportTestHelper.generateSupportToken(
                vendorKeyPair.getPrivate(), "TICKET-101", "Debug DB lock", Duration.ofHours(1)
        );

        // Verify succeeds while active
        assertNotNull(authentication.verify(new AuthVerifyRequestDTO(token)));

        // Revoke access
        supportAccessService.revokeAccess();

        // Verify immediately fails
        ConstraintException ex = assertThrows(ConstraintException.class, () ->
                authentication.verify(new AuthVerifyRequestDTO(token))
        );
        assertEquals(ConstraintErrors.SUPPORT_ACCESS_DISABLED.getIndirectionCode(), ex.getError().getIndirectionCode());
    }
}
