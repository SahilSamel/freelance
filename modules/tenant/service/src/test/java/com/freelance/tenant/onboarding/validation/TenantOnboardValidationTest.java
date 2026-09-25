package com.freelance.tenant.onboarding.validation;

import com.freelance.keys.tenant.TenantValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantOnboardValidation Unit Tests")
class TenantOnboardValidationTest {

    private TenantOnboardValidation validation;
    private TransactionContext context;

    @BeforeEach
    void setUp() {
        validation = new TenantOnboardValidation();
        context = TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Valid tenant create request passes validation")
    void testValidRequestPasses() {
        TenantCreateRequestDTO request = new TenantCreateRequestDTO("client1", "Client One");
        assertDoesNotThrow(() -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Null request raises INVALID_TENANT_ID")
    void testNullRequestFails() {
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().containsKey("tenantId"));
        assertTrue(context.getValidationErrors().get("tenantId").contains(TenantValidationErrors.INVALID_TENANT_ID));
    }

    @Test
    @DisplayName("Blank tenant ID raises INVALID_TENANT_ID")
    void testBlankTenantIdFails() {
        TenantCreateRequestDTO request = new TenantCreateRequestDTO("   ", "Client One");
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("tenantId"));
        assertTrue(context.getValidationErrors().get("tenantId").contains(TenantValidationErrors.INVALID_TENANT_ID));
    }

    @Test
    @DisplayName("Invalid characters in tenant ID raises INVALID_TENANT_ID_FORMAT")
    void testInvalidFormatFails() {
        TenantCreateRequestDTO request = new TenantCreateRequestDTO("inv@lid!tenant", "Client One");
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("tenantId"));
        assertTrue(context.getValidationErrors().get("tenantId").contains(TenantValidationErrors.INVALID_TENANT_ID_FORMAT));
    }

    @Test
    @DisplayName("Reserved tenant ID raises RESERVED_TENANT_ID")
    void testReservedTenantIdFails() {
        TenantCreateRequestDTO request = new TenantCreateRequestDTO("freelance", "Client One");
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("tenantId"));
        assertTrue(context.getValidationErrors().get("tenantId").contains(TenantValidationErrors.RESERVED_TENANT_ID));
    }

    @Test
    @DisplayName("Tenant name exceeding 255 chars raises INVALID_TENANT_NAME")
    void testNameTooLongFails() {
        String longName = "A".repeat(256);
        TenantCreateRequestDTO request = new TenantCreateRequestDTO("client1", longName);
        ValidationException ex = assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("name"));
        assertTrue(context.getValidationErrors().get("name").contains(TenantValidationErrors.INVALID_TENANT_NAME));
    }
}
