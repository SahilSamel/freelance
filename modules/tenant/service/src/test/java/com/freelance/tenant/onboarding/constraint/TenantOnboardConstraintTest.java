package com.freelance.tenant.onboarding.constraint;

import com.freelance.keys.tenant.TenantConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.fixtures.FakeTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantOnboardConstraint Unit Tests")
class TenantOnboardConstraintTest {

    private FakeTenantRepository tenantRepository;
    private TenantOnboardConstraint constraint;

    @BeforeEach
    void setUp() {
        tenantRepository = new FakeTenantRepository();
        constraint = new TenantOnboardConstraint(tenantRepository);
    }

    @Test
    @DisplayName("Null or blank request does nothing")
    void testNullOrBlankDoesNothing() {
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
        assertDoesNotThrow(() -> constraint.checkConstraints(new TenantCreateRequestDTO(null, "name")));
        assertDoesNotThrow(() -> constraint.checkConstraints(new TenantCreateRequestDTO("   ", "name")));
    }

    @Test
    @DisplayName("Non-existent tenant passes constraint check")
    void testNonExistentTenantPasses() {
        assertDoesNotThrow(() -> constraint.checkConstraints(new TenantCreateRequestDTO("newtenant", "New Tenant")));
    }

    @Test
    @DisplayName("Already existing tenant throws TENANT_ALREADY_EXISTS")
    void testExistingTenantThrows() {
        tenantRepository.save(new TenantEntity("existingtenant", "Existing", "ACTIVE"));

        ConstraintException ex = assertThrows(
                ConstraintException.class,
                () -> constraint.checkConstraints(new TenantCreateRequestDTO("existingtenant", "Existing"))
        );

        assertEquals(TenantConstraintErrors.TENANT_ALREADY_EXISTS, ex.getError());
    }
}
