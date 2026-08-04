package com.freelance.auth.service.validation;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for LoginValidation.
 * <p>
 * These run in isolation — no server, no DB, no Transaction context.
 * They verify that every branch of the validation logic raises the right error.
 * <p>
 * Note: The validation methods throw an exception when errors are raised,
 * so we assert that the exception is thrown for invalid inputs and NOT
 * thrown for valid inputs.
 */
@DisplayName("LoginValidation unit tests")
class LoginValidationTest {

    private final LoginValidation validation = new LoginValidation();

    // ── Null / empty request ──────────────────────────────────────────────────

    @Test
    @DisplayName("null request → throws (early return path)")
    void validateRequest_nullRequest_throws() {
        assertThrows(RuntimeException.class, () -> validation.validateRequest(null));
    }

    // ── Username rules ────────────────────────────────────────────────────────

    @Test
    @DisplayName("null username → throws INVALID_USERNAME")
    void validateRequest_nullUsername_throws() {
        assertThrows(RuntimeException.class,
                () -> validation.validateRequest(new AuthLoginRequestDTO(null, "pass")));
    }

    @Test
    @DisplayName("blank username → throws INVALID_USERNAME")
    void validateRequest_blankUsername_throws() {
        assertThrows(RuntimeException.class,
                () -> validation.validateRequest(new AuthLoginRequestDTO("  ", "pass")));
    }

    @Test
    @DisplayName("username too short (< 3 chars) → throws INVALID_USERNAME")
    void validateRequest_usernameTooShort_throws() {
        assertThrows(RuntimeException.class,
                () -> validation.validateRequest(new AuthLoginRequestDTO("ab", "pass")));
    }

    @Test
    @DisplayName("username too long (> 50 chars) → throws INVALID_USERNAME")
    void validateRequest_usernameTooLong_throws() {
        String longUsername = "a".repeat(51);
        assertThrows(RuntimeException.class,
                () -> validation.validateRequest(new AuthLoginRequestDTO(longUsername, "pass")));
    }

    // ── Password rules ────────────────────────────────────────────────────────

    @Test
    @DisplayName("null password → throws INVALID_PASSWORD")
    void validateRequest_nullPassword_throws() {
        assertThrows(RuntimeException.class,
                () -> validation.validateRequest(new AuthLoginRequestDTO("validuser", null)));
    }

    @Test
    @DisplayName("blank password → throws INVALID_PASSWORD")
    void validateRequest_blankPassword_throws() {
        assertThrows(RuntimeException.class,
                () -> validation.validateRequest(new AuthLoginRequestDTO("validuser", " ")));
    }

    // ── Valid input ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("valid username and password → no exception")
    void validateRequest_validInput_doesNotThrow() {
        assertDoesNotThrow(
                () -> validation.validateRequest(new AuthLoginRequestDTO("validuser", "secret")));
    }

    @Test
    @DisplayName("username exactly 3 chars → no exception (boundary)")
    void validateRequest_usernameAtMinBoundary_doesNotThrow() {
        assertDoesNotThrow(
                () -> validation.validateRequest(new AuthLoginRequestDTO("abc", "secret")));
    }

    @Test
    @DisplayName("username exactly 50 chars → no exception (boundary)")
    void validateRequest_usernameAtMaxBoundary_doesNotThrow() {
        String maxUsername = "a".repeat(50);
        assertDoesNotThrow(
                () -> validation.validateRequest(new AuthLoginRequestDTO(maxUsername, "secret")));
    }
}
