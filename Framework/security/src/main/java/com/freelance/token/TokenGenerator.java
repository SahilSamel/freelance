package com.freelance.token;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates cryptographically secure random tokens and numeric codes,
 * suitable for session tokens, password reset codes, and similar
 * one-time secrets. Not for password hashing — see PasswordHasher.
 */
public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_TOKEN_BYTES = 32;

    private TokenGenerator() {
        // Enforce static utility pattern
    }

    /**
     * Generates a URL-safe random token using the default length
     * (32 bytes, ~43 characters encoded).
     */
    public static String generateToken() {
        return generateToken(DEFAULT_TOKEN_BYTES);
    }

    /**
     * Generates a URL-safe random token of the given byte length before
     * encoding (the encoded string will be longer than byteLength).
     *
     * @param byteLength number of random bytes to generate; must be positive
     */
    public static String generateToken(int byteLength) {
        if (byteLength <= 0) {
            throw new IllegalArgumentException("Token byte length must be positive");
        }
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generates a random numeric code of the given length (e.g. for a
     * 6-digit email verification or password reset code). Each digit is
     * 0-9; leading zeros are possible.
     *
     * @param digits number of digits; must be positive
     */
    public static String generateNumericCode(int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException("Digit count must be positive");
        }
        StringBuilder code = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }
}
