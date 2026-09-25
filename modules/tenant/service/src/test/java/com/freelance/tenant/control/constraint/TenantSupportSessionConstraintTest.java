package com.freelance.tenant.control.constraint;

import com.freelance.keys.tenant.TenantConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.jaxrs.tenant.TenantRegistry;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.fixtures.FakeTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantSupportSessionConstraint Unit Tests")
class TenantSupportSessionConstraintTest {

    private FakeTenantRepository tenantRepository;
    private TenantSupportSessionConstraint constraint;

    @BeforeEach
    void setUp() {
        tenantRepository = new FakeTenantRepository();
        constraint = new TenantSupportSessionConstraint(tenantRepository);
    }

    @Test
    @DisplayName("Null or blank tenantId passes without exception")
    void testNullOrBlankDoesNothing() {
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
        assertDoesNotThrow(() -> constraint.checkConstraints("   "));
    }

    @Test
    @DisplayName("Non-existent tenant throws TENANT_NOT_FOUND")
    void testNonExistentTenantThrows() {
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints("unknown"));
        assertEquals(TenantConstraintErrors.TENANT_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Deactivated tenant throws TENANT_NOT_ACTIVE")
    void testDeactivatedTenantThrows() {
        TenantEntity deactivatedEntity = new TenantEntity("gym1", "Gym One", TenantRegistry.STATUS_DEACTIVATED);
        tenantRepository.save(deactivatedEntity);

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints("gym1"));
        assertEquals(TenantConstraintErrors.TENANT_NOT_ACTIVE, ex.getError());
    }

    @Test
    @DisplayName("Active tenant passes constraint check")
    void testActiveTenantPasses() {
        TenantEntity activeEntity = new TenantEntity("gym1", "Gym One", TenantRegistry.STATUS_ACTIVE);
        tenantRepository.save(activeEntity);

        assertDoesNotThrow(() -> constraint.checkConstraints("gym1"));
    }
}
