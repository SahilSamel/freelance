package com.freelance.auth.token;

import com.freelance.ConfigurationEngine;
import com.freelance.auth.dto.TokenGenerateRequestDTO;
import com.freelance.auth.dto.TokenGenerateResponseDTO;
import com.freelance.auth.dto.TokenParseRequestDTO;
import com.freelance.auth.dto.TokenParseResponseDTO;
import com.freelance.keys.authentication.AuthenticationConfigKeys;
import com.freelance.manager.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilityTest {

    private TokenUtility tokenUtility;

    @BeforeEach
    void setUp() throws Exception {
        tokenUtility = new TokenUtility();

        CacheManager mgr = CacheManager.getInstance();
        setField(mgr, "initialized", false);
        clearMap(mgr, "configsByName");
        clearMap(mgr, "nameByClass");
        clearMap(mgr, "caches");

        mgr.init("com.freelance.fixtures");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void clearMap(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        ((Map<?, ?>) f.get(target)).clear();
    }

    @Test
    @DisplayName("generateToken throws IllegalStateException when JWT secret is the default placeholder value")
    void generateToken_throwsWhenSecretIsDefault() {
        TokenGenerateRequestDTO request = new TokenGenerateRequestDTO(UUID.randomUUID(), "sample-session-key");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tokenUtility.generateToken(request)
        );

        assertTrue(exception.getMessage().contains("FATAL: JWT signing secret cannot be the default placeholder value"));
    }

    @Test
    @DisplayName("parse throws IllegalStateException when JWT secret is the default placeholder value")
    void parse_throwsWhenSecretIsDefault() {
        TokenParseRequestDTO request = new TokenParseRequestDTO("some.jwt.token");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tokenUtility.parse(request)
        );

        assertTrue(exception.getMessage().contains("FATAL: JWT signing secret cannot be the default placeholder value"));
    }

    @Test
    @DisplayName("generateToken and parse succeed when a valid custom JWT secret is configured")
    void generateAndParse_succeedWithCustomSecret() {
        String customSecret = "custom-super-secure-production-jwt-secret-key-123456789!";
        CacheManager.getInstance().put("cache_SystemConfig", AuthenticationConfigKeys.JWT_SECRET_KEY.getKey(), customSecret);

        UUID userId = UUID.randomUUID();
        String sessionKey = "session-12345";

        TokenGenerateResponseDTO generateResponse = tokenUtility.generateToken(
                new TokenGenerateRequestDTO(userId, sessionKey)
        );

        assertNotNull(generateResponse);
        assertNotNull(generateResponse.token());
        assertFalse(generateResponse.token().isBlank());

        TokenParseResponseDTO parseResponse = tokenUtility.parse(
                new TokenParseRequestDTO(generateResponse.token())
        );

        assertNotNull(parseResponse);
        assertEquals(userId, parseResponse.userId());
        assertEquals(sessionKey, parseResponse.sessionKey());
    }
}
