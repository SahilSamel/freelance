package com.freelance.tenant.control.validation;

import com.freelance.keys.tenant.TenantValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.tenant.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantDeactivateValidation Unit Tests")
class TenantDeactivateValidationTest {

    private TenantDeactivateValidation validation;
    private TransactionContext context;

    @BeforeEach
    void setUp() {
        validation = new TenantDeactivateValidation();
        context = TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Valid tenant ID passes validation")
    void testValidTenantIdPasses() {
        assertDoesNotThrow(() -> validation.validateRequest("customer1"));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Null tenant ID raises INVALID_TENANT_ID")
    void testNullTenantIdFails() {
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().containsKey("tenantId"));
        assertTrue(context.getValidationErrors().get("tenantId").contains(TenantValidationErrors.INVALID_TENANT_ID));
    }

    @Test
    @DisplayName("Blank tenant ID raises INVALID_TENANT_ID")
    void testBlankTenantIdFails() {
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest("   "));
        assertTrue(context.getValidationErrors().containsKey("tenantId"));
        assertTrue(context.getValidationErrors().get("tenantId").contains(TenantValidationErrors.INVALID_TENANT_ID));
    }

    @Test
    @DisplayName("Invalid characters in tenant ID raises INVALID_TENANT_ID_FORMAT")
    void testInvalidFormatFails() {
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest("inv@lid!tenant"));
        assertTrue(context.getValidationErrors().containsKey("tenantId"));
        assertTrue(context.getValidationErrors().get("tenantId").contains(TenantValidationErrors.INVALID_TENANT_ID_FORMAT));
    }
}
