package com.freelance.authorization.service;

import com.freelance.fixtures.TestContextHelper;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthorizationService isAuthorized Tests")
class AuthorizationServiceVerifyTest {

    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp(UUID.randomUUID());
        service = new AuthorizationService();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("isAuthorized allows request without checks when authorization is disabled")
    void isAuthorized_whenDisabled_allowsRequest() {
        CacheManager.getInstance().put("cache_SystemConfig", "authorization.enabled", "false");

        // Even with no authorization key in request context, it should pass without throwing
        Transaction.getRequestContext().withAuthorizationKey(null);

        assertDoesNotThrow(() -> service.isAuthorized());
    }

    @Test
    @DisplayName("isAuthorized throws validation error when authorization is enabled and authKey is missing")
    void isAuthorized_whenEnabledAndMissingKey_throwsValidationException() {
        CacheManager.getInstance().put("cache_SystemConfig", "authorization.enabled", "true");

        Transaction.getRequestContext().withAuthorizationKey(null);

        assertThrows(ValidationException.class, () -> service.isAuthorized());
    }
}
