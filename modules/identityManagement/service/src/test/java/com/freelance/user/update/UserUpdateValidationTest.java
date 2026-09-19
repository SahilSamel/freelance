package com.freelance.user.update;

import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.identityManagement.user.UserValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.user.update.validation.UserUpdateValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserUpdateValidation Tests")
class UserUpdateValidationTest {

    private TransactionContext context;
    private UserUpdateValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new UserUpdateValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid request passes validation")
    void validRequest_passes() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(UUID.randomUUID(), "updated_user", "NewPassword123");
        assertDoesNotThrow(() -> validation.validateRequest(request));
    }

    @Test
    @DisplayName("null request raises INVALID_USER_ID")
    void nullRequest_raisesError() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().get("id").contains(UserValidationErrors.INVALID_USER_ID));
    }

    @Test
    @DisplayName("null id raises INVALID_USER_ID")
    void nullId_raisesError() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(null, "updated_user", "NewPassword123");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("id").contains(UserValidationErrors.INVALID_USER_ID));
    }

    @Test
    @DisplayName("invalid username raises validation error")
    void invalidUsername_raisesError() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(UUID.randomUUID(), "ab", null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.USERNAME_TOO_SHORT));
    }

    @Test
    @DisplayName("short password raises PASSWORD_TOO_SHORT")
    void shortPassword_raisesError() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(UUID.randomUUID(), null, "123");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("password").contains(UserValidationErrors.PASSWORD_TOO_SHORT));
    }

    @Test
    @DisplayName("blank username raises INVALID_USERNAME")
    void blankUsername_raisesError() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(UUID.randomUUID(), "   ", null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.INVALID_USERNAME));
    }

    @Test
    @DisplayName("too long username raises USERNAME_TOO_LONG")
    void longUsername_raisesError() {
        String longUsername = "u".repeat(55);
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(UUID.randomUUID(), longUsername, null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.USERNAME_TOO_LONG));
    }

    @Test
    @DisplayName("invalid pattern username raises USERNAME_HAS_INVALID_CHARS")
    void invalidPatternUsername_raisesError() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(UUID.randomUUID(), "user!@#$%", null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.USERNAME_HAS_INVALID_CHARS));
    }

    @Test
    @DisplayName("blank password raises INVALID_PASSWORD")
    void blankPassword_raisesError() {
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(UUID.randomUUID(), null, "   ");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("password").contains(UserValidationErrors.INVALID_PASSWORD));
    }
}
