package com.freelance.auth.token;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecretManagerTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        JwtSecretManager.resetForTesting();
    }

    @Test
    @DisplayName("getSecretKey generates 256-bit ephemeral key when uninitialized")
    void testGetSecretKeyGeneratesEphemeralKey() {
        SecretKey key = JwtSecretManager.getSecretKey();
        assertNotNull(key);
        assertEquals("HmacSHA256", key.getAlgorithm());
        assertTrue(key.getEncoded().length >= 32, "Key must have at least 256 bits (32 bytes)");
    }

    @Test
    @DisplayName("getSecretKey returns the same singleton key across multiple calls in the same lifecycle")
    void testGetSecretKeyIsSingleton() {
        SecretKey key1 = JwtSecretManager.getSecretKey();
        SecretKey key2 = JwtSecretManager.getSecretKey();
        assertSame(key1, key2);
    }

    @Test
    @DisplayName("setSecretForTesting allows setting a custom key for testing")
    void testSetSecretForTesting() {
        String customSecret = "a-very-secure-custom-test-secret-that-is-long-enough!";
        JwtSecretManager.setSecretForTesting(customSecret);

        SecretKey key = JwtSecretManager.getSecretKey();
        assertNotNull(key);
        assertArrayEquals(customSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), key.getEncoded());
    }

    @Test
    @DisplayName("setSecretForTesting throws exception if secret is under 32 bytes")
    void testSetSecretForTestingTooShort() {
        assertThrows(IllegalArgumentException.class, () ->
                JwtSecretManager.setSecretForTesting("short-secret")
        );
    }

    @Test
    @DisplayName("resetForTesting clears key so next call generates a new key")
    void testResetForTesting() {
        SecretKey key1 = JwtSecretManager.getSecretKey();
        JwtSecretManager.resetForTesting();
        SecretKey key2 = JwtSecretManager.getSecretKey();

        assertNotSame(key1, key2);
    }
}
