package com.freelance.auth.service.utility.password;

import org.mindrot.jbcrypt.BCrypt;

/**
 * One-way password hashing via BCrypt. There is no decrypt — hashes
 * cannot be reversed back into the original password, only verified
 * against a candidate password.
 */
public final class HashUtility {

    /**
     * Work factor parameter (logarithmic rounds) configuring hashing computational difficulty.
     */
    private static final int WORK_FACTOR = 12;

    private HashUtility() {
        // Enforce static utility pattern
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isBlank() || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
