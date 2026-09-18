package com.freelance.role.read;

import com.freelance.authorization.dto.role.list.RoleListRequestDTO;
import com.freelance.authorization.dto.role.list.RoleListResponseDTO;
import com.freelance.authorization.dto.role.read.RoleReadRequestDTO;
import com.freelance.authorization.dto.role.read.RoleReadResponseDTO;
import com.freelance.authorization.entity.Authorization;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.role.entity.Role;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleReadService & RoleListService Tests")
class RoleReadServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("readRole by ID returns role and assigned authorizations including for system roles")
    void readRole_byId_returnsRoleWithAuths() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();

        UUID roleId = UUID.randomUUID();
        Role role = new Role(roleId, "SUPER_ADMIN", "Super administrator", true);
        roleRepo.persist(role);

        UUID authId = UUID.randomUUID();
        Authorization auth = new Authorization(authId, "USER_CREATE", "Create User", "Desc", "IdentityManagement");
        roleAuthRepo.storage.put(new com.freelance.mapping.roleAuthorization.entity.RoleAuthorizationKey(roleId, authId),
                new RoleAuthorization(role, auth));

        RoleReadService service = new RoleReadService(roleRepo, roleAuthRepo, new RoleAssembler());
        RoleReadResponseDTO response = service.readRole(new RoleReadRequestDTO(roleId, null));

        assertNotNull(response);
        assertNotNull(response.roleDTO());
        assertEquals("SUPER_ADMIN", response.roleDTO().name());
        assertTrue(response.roleDTO().isSystem());
        assertEquals(1, response.roleDTO().authorizations().size());
        assertEquals("USER_CREATE", response.roleDTO().authorizations().get(0).key());
    }

    @Test
    @DisplayName("readRole with unknown ID raises ROLE_NOT_FOUND")
    void readRole_unknown_raisesError() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();

        RoleReadService service = new RoleReadService(roleRepo, roleAuthRepo, new RoleAssembler());
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> service.readRole(new RoleReadRequestDTO(UUID.randomUUID(), null)));
        assertEquals(RoleConstraintErrors.ROLE_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("listRoles returns both system and custom roles")
    void listRoles_returnsAll() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();

        roleRepo.persist(new Role(UUID.randomUUID(), "SUPER_ADMIN", "System role", true));
        roleRepo.persist(new Role(UUID.randomUUID(), "CUSTOM_ROLE", "Custom role", false));

        RoleReadService service = new RoleReadService(roleRepo, roleAuthRepo, new RoleAssembler());
        RoleListResponseDTO response = service.listRoles(RoleListRequestDTO.empty());

        assertNotNull(response);
        assertEquals(2, response.roles().size());
    }
}
