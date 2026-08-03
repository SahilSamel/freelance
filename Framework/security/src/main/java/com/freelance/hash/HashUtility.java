package com.freelance.hash;

import org.mindrot.jbcrypt.BCrypt;

/**
 * One-way password hashing via BCrypt. There is no decrypt — hashes
 * cannot be reversed back into the original password, only verified
 * against a candidate password.
 */
public final class HashUtility {

    /**
     * BCrypt work factor. Higher is slower to compute but more resistant to brute force.
     */
    private static final int WORK_FACTOR = 12;

    private HashUtility() {
        // Enforce static utility pattern
    }

    /**
     * Hashes a plaintext password with a freshly generated random salt.
     * The salt is embedded in the returned hash string, so no separate
     * salt storage is needed.
     *
     * @param plainPassword the password to hash
     * @return a BCrypt hash string, safe to store directly
     * @throws IllegalArgumentException if plainPassword is null or blank
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Checks whether a plaintext password matches a previously stored hash.
     *
     * @param plainPassword the password to check
     * @param storedHash    the hash previously produced by hash(...)
     * @return true if the password matches, false otherwise (including if either input is null/blank)
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isBlank() || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // Thrown by jBCrypt if storedHash isn't a valid BCrypt hash (e.g. corrupted data).
            return false;
        }
    }
}
