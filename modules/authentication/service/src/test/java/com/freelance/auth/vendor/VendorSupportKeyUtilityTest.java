package com.freelance.auth.vendor;

import com.freelance.fixtures.VendorSupportTestHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class VendorSupportKeyUtilityTest {

    @Test
    @DisplayName("isVendorToken correctly identifies vendor support tokens")
    void testIsVendorToken() {
        KeyPair keyPair = VendorSupportTestHelper.generateRsaKeyPair();
        String token = VendorSupportTestHelper.generateSupportToken(
                keyPair.getPrivate(), "TICKET-1234", "Fixing production bug", Duration.ofHours(2)
        );

        assertTrue(VendorSupportKeyUtility.isVendorToken(token));
        assertFalse(VendorSupportKeyUtility.isVendorToken("invalid.token.here"));
        assertFalse(VendorSupportKeyUtility.isVendorToken(null));
        assertFalse(VendorSupportKeyUtility.isVendorToken(""));
    }

    @Test
    @DisplayName("parsePublicKey parses base64 RSA public key properly")
    void testParsePublicKey() {
        KeyPair keyPair = VendorSupportTestHelper.generateRsaKeyPair();
        String pubBase64 = VendorSupportTestHelper.encodeKeyToBase64(keyPair.getPublic());

        PublicKey parsedPub = VendorSupportKeyUtility.parsePublicKey(pubBase64);
        assertEquals(keyPair.getPublic(), parsedPub);
    }

    @Test
    @DisplayName("verifyAndParseSupportToken verifies and extracts claims with valid public key")
    void testVerifyAndParseSupportToken() {
        KeyPair keyPair = VendorSupportTestHelper.generateRsaKeyPair();
        String token = VendorSupportTestHelper.generateSupportToken(
                keyPair.getPrivate(), "TICKET-1234", "Fixing production bug", Duration.ofHours(2)
        );

        Claims claims = VendorSupportKeyUtility.verifyAndParseSupportToken(token, keyPair.getPublic());
        assertEquals("vendor-support", claims.getIssuer());
        assertEquals("vendor-tech-support", claims.getSubject());
        assertEquals("TICKET-1234", claims.get("ticketId"));
        assertEquals("Fixing production bug", claims.get("reason"));
    }

    @Test
    @DisplayName("verifyAndParseSupportToken fails with wrong public key")
    void testVerifyFailsWithWrongKey() {
        KeyPair keyPair1 = VendorSupportTestHelper.generateRsaKeyPair();
        KeyPair keyPair2 = VendorSupportTestHelper.generateRsaKeyPair();

        String token = VendorSupportTestHelper.generateSupportToken(
                keyPair1.getPrivate(), "TICKET-999", "Emergency audit", Duration.ofHours(1)
        );

        assertThrows(JwtException.class, () ->
                VendorSupportKeyUtility.verifyAndParseSupportToken(token, keyPair2.getPublic())
        );
    }
}
