package com.freelance.user.create;

import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.identityManagement.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.user.create.validation.UserCreateValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateValidation Tests")
class UserCreateValidationTest {

    private TransactionContext context;
    private UserCreateValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new UserCreateValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid request passes validation")
    void validRequest_passes() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "ValidPassword123");
        assertDoesNotThrow(() -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("null request raises validation error")
    void nullRequest_raisesErrors() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertFalse(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("blank username raises INVALID_USERNAME")
    void blankUsername_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("", "ValidPassword123");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(ValidationErrors.INVALID_USERNAME));
    }

    @Test
    @DisplayName("username too short raises USERNAME_TOO_SHORT")
    void shortUsername_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("ab", "ValidPassword123");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(ValidationErrors.USERNAME_TOO_SHORT));
    }

    @Test
    @DisplayName("username with invalid characters raises USERNAME_HAS_INVALID_CHARS")
    void invalidCharsUsername_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("user@name#", "ValidPassword123");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(ValidationErrors.USERNAME_HAS_INVALID_CHARS));
    }

    @Test
    @DisplayName("blank password raises INVALID_PASSWORD")
    void blankPassword_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("password").contains(ValidationErrors.INVALID_PASSWORD));
    }

    @Test
    @DisplayName("password too short raises PASSWORD_TOO_SHORT")
    void shortPassword_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "123");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("password").contains(ValidationErrors.PASSWORD_TOO_SHORT));
    }
}
