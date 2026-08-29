package com.freelance.utility.password;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HashUtility Tests")
class HashUtilityTest {

    @Test
    @DisplayName("hash() generates valid BCrypt hash and verify() verifies matching password")
    void hashAndVerify_success() {
        String password = "SecretPassword123";
        String hash = HashUtility.hash(password);

        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(HashUtility.verify(password, hash));
        assertFalse(HashUtility.verify("WrongPassword", hash));

        assertTrue(HashUtility.verify("Welcome@1", "$2a$12$KndVHQG4kfm7hiAId0JkJO9mH6CWKC5Jtd0tqV96.aHU2Jp0HjkRS"));
    }

    @Test
    @DisplayName("hash() throws IllegalArgumentException on blank or null input")
    void hash_throwsOnNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> HashUtility.hash(null));
        assertThrows(IllegalArgumentException.class, () -> HashUtility.hash("   "));
    }

    @Test
    @DisplayName("verify() returns false on null or blank input")
    void verify_returnsFalseOnNullOrBlank() {
        assertFalse(HashUtility.verify(null, "someHash"));
        assertFalse(HashUtility.verify("pass", null));
        assertFalse(HashUtility.verify("pass", ""));
        assertFalse(HashUtility.verify("", "someHash"));
    }
}
