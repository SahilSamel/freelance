package com.freelance.tenant.onboarding;

import com.freelance.config.DatabaseConfig;
import com.freelance.config.DatabaseConfigHolder;
import com.freelance.enums.DeploymentMode;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantOnboardingService Unit Tests")
class TenantOnboardingServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        DatabaseConfigHolder.set(new DatabaseConfig("jdbc:postgresql://localhost:5432/db", "user", "pass", DeploymentMode.TEST));
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Default constructor instantiates successfully")
    void testInstantiation() {
        TenantOnboardingService service = new TenantOnboardingService();
        assertNotNull(service);
    }

    @Test
    @DisplayName("Invalid request throws ValidationException")
    void testInvalidRequestThrowsValidationException() {
        TenantOnboardingService service = new TenantOnboardingService();
        assertThrows(ValidationException.class, () -> service.onboardTenant(new TenantCreateRequestDTO("", "Name")));
        assertThrows(ValidationException.class, () -> service.onboardTenant(null));
    }
}
