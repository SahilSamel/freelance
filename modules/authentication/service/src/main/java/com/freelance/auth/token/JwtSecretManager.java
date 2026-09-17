package com.freelance.auth.token;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Manages the lifecycle of the in-memory ephemeral HMAC-SHA256 signing key for user session tokens.
 * <p>
 * Operating under a zero-configuration model, a cryptographically strong 256-bit secret key is
 * generated dynamically in memory upon application startup via {@link Jwts.SIG#HS256}.
 * The secret is never stored in database tables, config files, or external caches, preventing
 * secret exposure or leakage.
 * </p>
 */
public final class JwtSecretManager {

    private static final Logger logger = LoggerFactory.getLogger(JwtSecretManager.class);
    private static volatile SecretKey secretKey;

    private JwtSecretManager() {
    }

    /**
     * Initializes the in-memory ephemeral signing key if not already created.
     */
    public static synchronized void initialize() {
        if (secretKey == null) {
            secretKey = Jwts.SIG.HS256.key().build();
            logger.info("[JwtSecretManager] Generated ephemeral 256-bit HMAC-SHA256 signing key in memory.");
        }
    }

    /**
     * Retrieves the active in-memory secret key, lazily initializing if necessary.
     *
     * @return active {@link SecretKey}
     */
    public static SecretKey getSecretKey() {
        if (secretKey == null) {
            synchronized (JwtSecretManager.class) {
                if (secretKey == null) {
                    initialize();
                }
            }
        }
        return secretKey;
    }

    /**
     * Overrides the active secret key for unit and integration testing purposes.
     *
     * @param customKey the secret key to use during test execution
     */
    public static synchronized void setSecretForTesting(SecretKey customKey) {
        secretKey = customKey;
    }

    /**
     * Overrides the active secret key with a specific UTF-8 string for testing.
     *
     * @param secretString secret string of at least 32 bytes
     */
    public static synchronized void setSecretForTesting(String secretString) {
        if (secretString == null || secretString.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Test secret must be at least 32 bytes long.");
        }
        secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Resets the active secret key so the next invocation generates a fresh key.
     */
    public static synchronized void resetForTesting() {
        secretKey = null;
    }
}
