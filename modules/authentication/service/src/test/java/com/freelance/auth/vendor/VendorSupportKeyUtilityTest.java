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
                keyPair.getPrivate(), Duration.ofHours(2)
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
                keyPair.getPrivate(), Duration.ofHours(2)
        );

        Claims claims = VendorSupportKeyUtility.verifyAndParseSupportToken(token, keyPair.getPublic());
        assertEquals("vendor-support", claims.getIssuer());
        assertEquals(com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_USER_ID.asString(), claims.getSubject());
        assertEquals(com.freelance.system.SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString(), claims.get("sessionKey"));
    }

    @Test
    @DisplayName("verifyAndParseSupportToken fails with wrong public key")
    void testVerifyFailsWithWrongKey() {
        KeyPair keyPair1 = VendorSupportTestHelper.generateRsaKeyPair();
        KeyPair keyPair2 = VendorSupportTestHelper.generateRsaKeyPair();

        String token = VendorSupportTestHelper.generateSupportToken(
                keyPair1.getPrivate(), Duration.ofHours(1)
        );

        assertThrows(JwtException.class, () ->
                VendorSupportKeyUtility.verifyAndParseSupportToken(token, keyPair2.getPublic())
        );
    }

    @Test
    @DisplayName("parsePublicKey supports PEM headers and whitespace")
    void testParsePublicKeyWithPem() {
        KeyPair keyPair = VendorSupportTestHelper.generateRsaKeyPair();
        String pubBase64 = VendorSupportTestHelper.encodeKeyToBase64(keyPair.getPublic());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + pubBase64 + "\n-----END PUBLIC KEY-----";

        PublicKey parsedPub = VendorSupportKeyUtility.parsePublicKey(pem);
        assertEquals(keyPair.getPublic(), parsedPub);
    }

    @Test
    @DisplayName("parsePublicKey throws IllegalArgumentException for null, blank, or invalid base64")
    void testParsePublicKeyInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> VendorSupportKeyUtility.parsePublicKey(null));
        assertThrows(IllegalArgumentException.class, () -> VendorSupportKeyUtility.parsePublicKey("   "));
        assertThrows(IllegalArgumentException.class, () -> VendorSupportKeyUtility.parsePublicKey("NOT_A_VALID_KEY_STRING"));
    }

    @Test
    @DisplayName("verifyAndParseSupportToken throws IllegalArgumentException or IllegalStateException for missing token/key")
    void testVerifyAndParseInvalidArguments() {
        KeyPair keyPair = VendorSupportTestHelper.generateRsaKeyPair();
        assertThrows(IllegalArgumentException.class, () -> VendorSupportKeyUtility.verifyAndParseSupportToken(null, keyPair.getPublic()));
        assertThrows(IllegalArgumentException.class, () -> VendorSupportKeyUtility.verifyAndParseSupportToken("   ", keyPair.getPublic()));
        assertThrows(IllegalStateException.class, () -> VendorSupportKeyUtility.verifyAndParseSupportToken("dummy.token.here", null));
    }

    @Test
    @DisplayName("Private constructor reflection test")
    void testConstructor() throws Exception {
        java.lang.reflect.Constructor<VendorSupportKeyUtility> ctor = VendorSupportKeyUtility.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        VendorSupportKeyUtility instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
