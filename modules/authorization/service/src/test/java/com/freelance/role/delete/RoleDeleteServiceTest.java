package com.freelance.role.delete;

import com.freelance.authorization.dto.role.delete.RoleDeleteRequestDTO;
import com.freelance.authorization.dto.role.delete.RoleDeleteResponseDTO;
import com.freelance.role.entity.Role;
import com.freelance.fixtures.FakeRoleAuthorizationRepository;
import com.freelance.fixtures.FakeRoleRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleDeleteService Tests")
class RoleDeleteServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("attempting to delete a system role raises SYSTEM_ROLE_CANNOT_BE_MANAGED")
    void deleteSystemRole_raisesError() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();

        UUID systemRoleId = UUID.randomUUID();
        roleRepo.persist(new Role(systemRoleId, "SUPER_ADMIN", "System admin", true));

        RoleDeleteService service = new RoleDeleteService(roleRepo, roleAuthRepo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> service.deleteRole(new RoleDeleteRequestDTO(systemRoleId)));
        assertEquals(RoleConstraintErrors.SYSTEM_ROLE_CANNOT_BE_MANAGED, ex.getError());
    }

    @Test
    @DisplayName("deleting a custom role removes role and mappings")
    void deleteCustomRole_success() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();

        UUID customRoleId = UUID.randomUUID();
        roleRepo.persist(new Role(customRoleId, "CUSTOM_ROLE", "Custom role", false));

        RoleDeleteService service = new RoleDeleteService(roleRepo, roleAuthRepo);
        RoleDeleteResponseDTO response = service.deleteRole(new RoleDeleteRequestDTO(customRoleId));

        assertNotNull(response);
        assertEquals(customRoleId, response.id());
        assertTrue(response.deleted());
        assertEquals(1, roleRepo.removeCalls);
        assertFalse(roleRepo.findById(customRoleId).isPresent());
    }
}
