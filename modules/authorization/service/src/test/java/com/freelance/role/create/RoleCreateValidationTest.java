package com.freelance.role.create;

import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authorization.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.role.create.validation.RoleCreateValidation;
import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleCreateValidation Tests")
class RoleCreateValidationTest {

    private TransactionContext context;
    private RoleCreateValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new RoleCreateValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid role request passes validation")
    void validRequest_passes() {
        RoleCreateRequestDTO request = new RoleCreateRequestDTO("ROLE_EDITOR", "Editor role", Collections.emptyList());
        assertDoesNotThrow(() -> validation.validateRequest(request));
    }

    @Test
    @DisplayName("null request raises INVALID_ROLE_NAME")
    void nullRequest_raisesError() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().get("name").contains(ValidationErrors.INVALID_ROLE_NAME));
    }

    @Test
    @DisplayName("blank name raises INVALID_ROLE_NAME")
    void blankName_raisesError() {
        RoleCreateRequestDTO request = new RoleCreateRequestDTO("  ", "Desc", Collections.emptyList());
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("name").contains(ValidationErrors.INVALID_ROLE_NAME));
    }

    @Test
    @DisplayName("name too short raises ROLE_NAME_TOO_SHORT")
    void shortName_raisesError() {
        RoleCreateRequestDTO request = new RoleCreateRequestDTO("ab", "Desc", Collections.emptyList());
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("name").contains(ValidationErrors.ROLE_NAME_TOO_SHORT));
    }
}
