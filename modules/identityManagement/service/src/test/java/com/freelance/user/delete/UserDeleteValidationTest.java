package com.freelance.user.delete;

import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.identityManagement.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.user.delete.validation.UserDeleteValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserDeleteValidation Tests")
class UserDeleteValidationTest {

    private TransactionContext context;
    private UserDeleteValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new UserDeleteValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid request passes validation")
    void validRequest_passes() {
        UserDeleteRequestDTO request = new UserDeleteRequestDTO(UUID.randomUUID());
        assertDoesNotThrow(() -> validation.validateRequest(request));
    }

    @Test
    @DisplayName("null request raises INVALID_USER_ID")
    void nullRequest_raisesError() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().get("id").contains(ValidationErrors.INVALID_USER_ID));
    }

    @Test
    @DisplayName("null id raises INVALID_USER_ID")
    void nullId_raisesError() {
        UserDeleteRequestDTO request = new UserDeleteRequestDTO(null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("id").contains(ValidationErrors.INVALID_USER_ID));
    }
}
