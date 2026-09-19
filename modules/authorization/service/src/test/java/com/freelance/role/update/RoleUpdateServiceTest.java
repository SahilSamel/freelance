package com.freelance.role.update;

import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.authorization.dto.role.update.RoleUpdateResponseDTO;
import com.freelance.authorization.entity.Authorization;
import com.freelance.role.entity.Role;
import com.freelance.fixtures.FakeAuthorizationRepository;
import com.freelance.fixtures.FakeRoleAuthorizationRepository;
import com.freelance.fixtures.FakeRoleRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.role.assembler.RoleAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleUpdateService Tests")
class RoleUpdateServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("attempting to update a system role raises SYSTEM_ROLE_CANNOT_BE_MANAGED")
    void updateSystemRole_raisesError() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();

        UUID systemRoleId = UUID.randomUUID();
        roleRepo.persist(new Role(systemRoleId, "SUPER_ADMIN", "System admin", true));

        RoleUpdateService service = new RoleUpdateService(roleRepo, roleAuthRepo, authRepo, new RoleAssembler());
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> service.updateRole(new RoleUpdateRequestDTO(systemRoleId, "MODIFIED_NAME", "desc", null)));
        assertEquals(RoleConstraintErrors.SYSTEM_ROLE_CANNOT_BE_MANAGED, ex.getError());
    }

    @Test
    @DisplayName("updating custom role name and authorizations succeeds")
    void updateCustomRole_success() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();

        UUID roleId = UUID.randomUUID();
        roleRepo.persist(new Role(roleId, "OLD_ROLE_NAME", "Old desc", false));

        UUID newAuthId = UUID.randomUUID();
        authRepo.storage.put(newAuthId, new Authorization(newAuthId, "NEW_AUTH", "New Auth", "Desc", "Group1"));

        RoleUpdateService service = new RoleUpdateService(roleRepo, roleAuthRepo, authRepo, new RoleAssembler());
        RoleUpdateResponseDTO response = service.updateRole(
                new RoleUpdateRequestDTO(roleId, "NEW_ROLE_NAME", "Updated desc", List.of(newAuthId))
        );

        assertNotNull(response);
        assertEquals("NEW_ROLE_NAME", response.name());
        assertEquals("Updated desc", response.description());
        assertEquals(1, response.authorizations().size());
        assertEquals("NEW_AUTH", response.authorizations().get(0).key());
    }

    @Test
    @DisplayName("updateRole with null authorizationIds preserves existing authorizations")
    void updateCustomRole_nullAuthIdsPreservesExisting() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();

        UUID roleId = UUID.randomUUID();
        Role role = new Role(roleId, "ROLE_TEST", "Desc", false);
        roleRepo.persist(role);

        UUID authId = UUID.randomUUID();
        Authorization auth = new Authorization(authId, "AUTH_EXIST", "Name", "Desc", "Group");
        authRepo.storage.put(authId, auth);
        roleAuthRepo.persist(new com.freelance.mapping.roleAuthorization.entity.RoleAuthorization(role, auth));

        RoleUpdateService service = new RoleUpdateService(roleRepo, roleAuthRepo, authRepo, new RoleAssembler());
        RoleUpdateResponseDTO response = service.updateRole(
                new RoleUpdateRequestDTO(roleId, "ROLE_TEST_UPDATED", null, null)
        );

        assertNotNull(response);
        assertEquals("ROLE_TEST_UPDATED", response.name());
        assertEquals(1, response.authorizations().size());
        assertEquals("AUTH_EXIST", response.authorizations().get(0).key());
    }

    @Test
    @DisplayName("Should test default constructor of RoleUpdateService")
    void testDefaultConstructor() {
        assertNotNull(new RoleUpdateService());
    }
}
