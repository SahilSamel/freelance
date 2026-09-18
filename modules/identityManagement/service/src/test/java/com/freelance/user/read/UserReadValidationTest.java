package com.freelance.user.read;

import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.identityManagement.user.UserValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.user.read.validation.UserReadValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserReadValidation Tests")
class UserReadValidationTest {

    private TransactionContext context;
    private UserReadValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new UserReadValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid request with id passes")
    void validWithId_passes() {
        UserReadRequestDTO request = new UserReadRequestDTO(UUID.randomUUID(), null);
        assertDoesNotThrow(() -> validation.validateRequest(request));
    }

    @Test
    @DisplayName("valid request with username passes")
    void validWithUsername_passes() {
        UserReadRequestDTO request = new UserReadRequestDTO(null, "some_user");
        assertDoesNotThrow(() -> validation.validateRequest(request));
    }

    @Test
    @DisplayName("request with neither id nor username raises INVALID_USER_ID")
    void emptyRequest_raisesError() {
        UserReadRequestDTO request = new UserReadRequestDTO(null, "");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("id").contains(UserValidationErrors.INVALID_USER_ID));
    }
}
