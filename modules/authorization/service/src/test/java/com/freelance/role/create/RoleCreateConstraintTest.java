package com.freelance.role.create;

import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.authorization.entity.Authorization;
import com.freelance.role.entity.Role;
import com.freelance.fixtures.FakeAuthorizationRepository;
import com.freelance.fixtures.FakeRoleRepository;
import com.freelance.keys.authorization.permission.PermissionConstraintErrors;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.role.create.constraint.RoleCreateConstraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleCreateConstraint Tests")
class RoleCreateConstraintTest {

    @Test
    @DisplayName("unique role name and valid authorizations pass")
    void uniqueNameAndValidAuths_pass() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();
        UUID authId = UUID.randomUUID();
        authRepo.storage.put(authId, new Authorization(authId, "AUTH1", "Auth 1", "Desc", "Group1"));

        RoleCreateConstraint constraint = new RoleCreateConstraint(roleRepo, authRepo);
        List<Authorization> result = constraint.checkConstraints(
                new RoleCreateRequestDTO("NEW_ROLE", "Desc", List.of(authId))
        );
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("duplicate role name raises ROLE_ALREADY_EXISTS")
    void duplicateName_raisesError() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        roleRepo.persist(new Role(UUID.randomUUID(), "EXISTING_ROLE", "Desc", false));

        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();
        RoleCreateConstraint constraint = new RoleCreateConstraint(roleRepo, authRepo);

        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new RoleCreateRequestDTO("EXISTING_ROLE", "Desc", null)));
        assertEquals(RoleConstraintErrors.ROLE_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("non-existent authorization id raises AUTHORIZATION_NOT_FOUND")
    void unknownAuth_raisesError() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();
        UUID nonExistentAuthId = UUID.randomUUID();

        RoleCreateConstraint constraint = new RoleCreateConstraint(roleRepo, authRepo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new RoleCreateRequestDTO("VALID_ROLE", "Desc", List.of(nonExistentAuthId))));
        assertEquals(PermissionConstraintErrors.AUTHORIZATION_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("null request returns empty list gracefully")
    void nullRequest_returnsEmptyList() {
        RoleCreateConstraint constraint = new RoleCreateConstraint();
        List<Authorization> res = constraint.checkConstraints(null);
        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    @DisplayName("null authorization list returns empty list and passes")
    void nullAuths_passes() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();
        RoleCreateConstraint constraint = new RoleCreateConstraint(roleRepo, authRepo);

        List<Authorization> res = constraint.checkConstraints(new RoleCreateRequestDTO("ROLE_NO_AUTHS", "Desc", null));
        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    @DisplayName("null element in authorization ids list raises AUTHORIZATION_NOT_FOUND")
    void nullElementInAuthList_raisesError() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();
        RoleCreateConstraint constraint = new RoleCreateConstraint(roleRepo, authRepo);

        List<UUID> authListWithNull = new java.util.ArrayList<>();
        authListWithNull.add(null);

        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new RoleCreateRequestDTO("ROLE_NULL_AUTH", "Desc", authListWithNull)));
        assertEquals(PermissionConstraintErrors.AUTHORIZATION_NOT_FOUND, ex.getError());
    }
}
