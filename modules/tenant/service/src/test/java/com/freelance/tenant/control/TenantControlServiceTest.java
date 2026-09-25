package com.freelance.tenant.control;

import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.tenant.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantControlService Unit Tests")
class TenantControlServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Default constructor instantiates successfully")
    void testInstantiation() {
        TenantControlService service = new TenantControlService();
        assertNotNull(service);
    }

    @Test
    @DisplayName("Deactivating freelance control plane throws ConstraintException")
    void testDeactivateFreelanceThrows() {
        TenantControlService service = new TenantControlService();
        assertThrows(ConstraintException.class, () -> service.deactivateTenant("freelance"));
        assertThrows(ConstraintException.class, () -> service.deactivateTenant("FREELANCE"));
    }

    @Test
    @DisplayName("Deactivating null or blank tenant throws ValidationException")
    void testDeactivateBlankThrows() {
        TenantControlService service = new TenantControlService();
        assertThrows(ValidationException.class, () -> service.deactivateTenant(null));
        assertThrows(ValidationException.class, () -> service.deactivateTenant("   "));
    }

    @Test
    @DisplayName("Generating support session for blank tenantId throws ValidationException")
    void testGenerateSupportSessionBlankTenantThrows() {
        TenantControlService service = new TenantControlService();
        assertThrows(ValidationException.class, () -> service.generateSupportSession(null));
        assertThrows(ValidationException.class, () -> service.generateSupportSession("  "));
    }
}
