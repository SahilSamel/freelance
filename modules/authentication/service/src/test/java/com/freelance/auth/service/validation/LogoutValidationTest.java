package com.freelance.auth.service.validation;

import com.freelance.auth.dto.AuthLogoutRequestDTO;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authentication.auth.AuthValidationErrors;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LogoutValidation Unit Tests")
class LogoutValidationTest {

    private LogoutValidation validation;
    private TransactionContext context;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new LogoutValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Valid logout request passes validation with no errors")
    void testValidRequest() {
        AuthLogoutRequestDTO req = new AuthLogoutRequestDTO("valid.jwt.token");
        validation.validateRequest(req);
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Null request raises validation error")
    void testNullRequest() {
        validation.validateRequest(null);
        assertFalse(context.getValidationErrors().isEmpty());
        assertTrue(context.getValidationErrors().get("token").contains(AuthValidationErrors.INVALID_TOKEN));
    }

    @Test
    @DisplayName("Null token in request raises validation error")
    void testNullToken() {
        AuthLogoutRequestDTO req = new AuthLogoutRequestDTO(null);
        validation.validateRequest(req);
        assertFalse(context.getValidationErrors().isEmpty());
        assertTrue(context.getValidationErrors().get("token").contains(AuthValidationErrors.INVALID_TOKEN));
    }
}
