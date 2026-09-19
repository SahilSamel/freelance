package com.freelance.role.delete;

import com.freelance.authorization.dto.role.delete.RoleDeleteRequestDTO;
import com.freelance.fixtures.FakeRoleRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.role.delete.constraint.RoleDeleteConstraint;
import com.freelance.role.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleDeleteConstraint Unit Tests")
class RoleDeleteConstraintTest {

    private FakeRoleRepository roleRepo;
    private RoleDeleteConstraint constraint;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        roleRepo = new FakeRoleRepository();
        constraint = new RoleDeleteConstraint(roleRepo);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Valid non-system role passes constraint check")
    void validDelete_passes() {
        UUID roleId = UUID.randomUUID();
        Role role = new Role(roleId, "CUSTOM_ROLE", "Custom Desc", false);
        roleRepo.persist(role);

        Role res = constraint.checkConstraints(new RoleDeleteRequestDTO(roleId));
        assertNotNull(res);
        assertEquals(roleId, res.getId());
    }

    @Test
    @DisplayName("Null request or null id returns null gracefully")
    void nullRequest_returnsNull() {
        assertNull(constraint.checkConstraints(null));
        assertNull(constraint.checkConstraints(new RoleDeleteRequestDTO(null)));
    }

    @Test
    @DisplayName("Throws ROLE_TO_DELETE_NOT_FOUND when role id does not exist")
    void roleNotFound_raisesError() {
        UUID missingId = UUID.randomUUID();
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new RoleDeleteRequestDTO(missingId)));
        assertEquals(RoleConstraintErrors.ROLE_TO_DELETE_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Throws SYSTEM_ROLE_CANNOT_BE_MANAGED when role is system-owned")
    void systemRole_raisesError() {
        UUID sysId = UUID.randomUUID();
        Role sysRole = new Role(sysId, "SUPER_ADMIN", "System Desc", true);
        roleRepo.persist(sysRole);

        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new RoleDeleteRequestDTO(sysId)));
        assertEquals(RoleConstraintErrors.SYSTEM_ROLE_CANNOT_BE_MANAGED, ex.getError());
    }

    @Test
    @DisplayName("Default 0-arg constructor instantiates successfully")
    void defaultConstructor() {
        RoleDeleteConstraint c = new RoleDeleteConstraint();
        assertNotNull(c);
    }
}
