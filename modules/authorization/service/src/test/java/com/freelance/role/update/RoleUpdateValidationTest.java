package com.freelance.role.update;

import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authorization.role.RoleValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.role.update.validation.RoleUpdateValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleUpdateValidation Unit Tests")
class RoleUpdateValidationTest {

    private TransactionContext context;
    private final RoleUpdateValidation validation = new RoleUpdateValidation();

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should fail when request is null")
    void testNullRequest() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().get("id").contains(RoleValidationErrors.INVALID_ROLE_ID));
    }

    @Test
    @DisplayName("Should fail when id is null")
    void testNullId() {
        assertThrows(ValidationException.class, () ->
                validation.validateRequest(new RoleUpdateRequestDTO(null, "VALID_NAME", "desc", null))
        );
        assertTrue(context.getValidationErrors().get("id").contains(RoleValidationErrors.INVALID_ROLE_ID));
    }

    @Test
    @DisplayName("Should fail when role name is blank")
    void testBlankName() {
        UUID id = UUID.randomUUID();
        assertThrows(ValidationException.class, () ->
                validation.validateRequest(new RoleUpdateRequestDTO(id, "   ", "desc", null))
        );
        assertTrue(context.getValidationErrors().get("name").contains(RoleValidationErrors.INVALID_ROLE_NAME));
    }

    @Test
    @DisplayName("Should fail when role name is too short")
    void testShortName() {
        UUID id = UUID.randomUUID();
        assertThrows(ValidationException.class, () ->
                validation.validateRequest(new RoleUpdateRequestDTO(id, "AB", "desc", null))
        );
        assertTrue(context.getValidationErrors().get("name").contains(RoleValidationErrors.ROLE_NAME_TOO_SHORT));
    }

    @Test
    @DisplayName("Should fail when role name is too long")
    void testLongName() {
        UUID id = UUID.randomUUID();
        String longName = "A".repeat(55);
        assertThrows(ValidationException.class, () ->
                validation.validateRequest(new RoleUpdateRequestDTO(id, longName, "desc", null))
        );
        assertTrue(context.getValidationErrors().get("name").contains(RoleValidationErrors.ROLE_NAME_TOO_LONG));
    }

    @Test
    @DisplayName("Should fail when role name contains invalid characters")
    void testInvalidChars() {
        UUID id = UUID.randomUUID();
        assertThrows(ValidationException.class, () ->
                validation.validateRequest(new RoleUpdateRequestDTO(id, "role@#$!%", "desc", null))
        );
        assertTrue(context.getValidationErrors().get("name").contains(RoleValidationErrors.ROLE_NAME_HAS_INVALID_CHARS));
    }

    @Test
    @DisplayName("Should pass when all fields are valid or name is null (updating only description/authorizations)")
    void testValidUpdatePasses() {
        UUID id = UUID.randomUUID();
        assertDoesNotThrow(() ->
                validation.validateRequest(new RoleUpdateRequestDTO(id, "VALID_ROLE_123", "Valid description", null))
        );
        assertDoesNotThrow(() ->
                validation.validateRequest(new RoleUpdateRequestDTO(id, null, "Updated description only", null))
        );
    }
}
