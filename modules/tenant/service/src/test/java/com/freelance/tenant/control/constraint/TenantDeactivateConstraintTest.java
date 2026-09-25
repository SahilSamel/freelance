package com.freelance.tenant.control.constraint;

import com.freelance.keys.tenant.TenantConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.fixtures.FakeTenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantDeactivateConstraint Unit Tests")
class TenantDeactivateConstraintTest {

    private FakeTenantRepository tenantRepository;
    private TenantDeactivateConstraint constraint;

    @BeforeEach
    void setUp() {
        tenantRepository = new FakeTenantRepository();
        constraint = new TenantDeactivateConstraint(tenantRepository);
    }

    @Test
    @DisplayName("Deactivating control plane throws CANNOT_DEACTIVATE_CONTROL_PLANE")
    void testDeactivateControlPlaneThrows() {
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints("freelance"));
        assertEquals(TenantConstraintErrors.CANNOT_DEACTIVATE_CONTROL_PLANE, ex.getError());

        ConstraintException exUpper = assertThrows(ConstraintException.class, () -> constraint.checkConstraints("FREELANCE"));
        assertEquals(TenantConstraintErrors.CANNOT_DEACTIVATE_CONTROL_PLANE, exUpper.getError());
    }

    @Test
    @DisplayName("Non-existent tenant throws TENANT_NOT_FOUND")
    void testNonExistentTenantThrows() {
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints("unknown"));
        assertEquals(TenantConstraintErrors.TENANT_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Existing active tenant returns entity")
    void testExistingTenantReturnsEntity() {
        TenantEntity entity = new TenantEntity("client1", "Client One", "ACTIVE");
        tenantRepository.save(entity);

        TenantEntity result = constraint.checkConstraints("client1");
        assertNotNull(result);
        assertEquals("client1", result.getTenantId());
    }
}
