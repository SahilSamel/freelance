package com.freelance.role.update;

import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.authorization.entity.Authorization;
import com.freelance.fixtures.FakeAuthorizationRepository;
import com.freelance.fixtures.FakeRoleRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authorization.permission.PermissionConstraintErrors;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.role.entity.Role;
import com.freelance.role.update.constraint.RoleUpdateConstraint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleUpdateConstraint Unit Tests")
class RoleUpdateConstraintTest {

    private FakeRoleRepository roleRepo;
    private FakeAuthorizationRepository authRepo;
    private RoleUpdateConstraint constraint;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        roleRepo = new FakeRoleRepository();
        authRepo = new FakeAuthorizationRepository();
        constraint = new RoleUpdateConstraint(roleRepo, authRepo);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Returns null when request or id is null")
    void testNullRequestAndId() {
        assertNull(constraint.checkConstraints(null, new ArrayList<>()));
        assertNull(constraint.checkConstraints(new RoleUpdateRequestDTO(null, "NAME", "desc", null), new ArrayList<>()));
    }

    @Test
    @DisplayName("Throws ROLE_TO_UPDATE_NOT_FOUND when role id does not exist")
    void testRoleNotFound() {
        RoleUpdateRequestDTO req = new RoleUpdateRequestDTO(UUID.randomUUID(), "NAME", "desc", null);
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req, new ArrayList<>()));
        assertEquals(RoleConstraintErrors.ROLE_TO_UPDATE_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Throws SYSTEM_ROLE_CANNOT_BE_MANAGED when role is a system role")
    void testSystemRole() {
        UUID sysId = UUID.randomUUID();
        roleRepo.persist(new Role(sysId, "SUPER_ADMIN", "desc", true));

        RoleUpdateRequestDTO req = new RoleUpdateRequestDTO(sysId, "SUPER_ADMIN_EDITED", "desc", null);
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req, new ArrayList<>()));
        assertEquals(RoleConstraintErrors.SYSTEM_ROLE_CANNOT_BE_MANAGED, ex.getError());
    }

    @Test
    @DisplayName("Throws ROLE_ALREADY_EXISTS when another role with the same name exists")
    void testRoleNameDuplicate() {
        UUID id1 = UUID.randomUUID();
        roleRepo.persist(new Role(id1, "ROLE_ONE", "desc", false));

        UUID id2 = UUID.randomUUID();
        roleRepo.persist(new Role(id2, "ROLE_TWO", "desc", false));

        // Try to rename ROLE_TWO to ROLE_ONE
        RoleUpdateRequestDTO req = new RoleUpdateRequestDTO(id2, "ROLE_ONE", "desc", null);
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req, new ArrayList<>()));
        assertEquals(RoleConstraintErrors.ROLE_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("Throws AUTHORIZATION_NOT_FOUND when authorization id does not exist in repository")
    void testAuthorizationNotFound() {
        UUID roleId = UUID.randomUUID();
        roleRepo.persist(new Role(roleId, "ROLE_ONE", "desc", false));

        UUID badAuthId = UUID.randomUUID();
        RoleUpdateRequestDTO req = new RoleUpdateRequestDTO(roleId, "ROLE_ONE", "desc", List.of(badAuthId));
        List<Authorization> outAuths = new ArrayList<>();

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req, outAuths));
        assertEquals(PermissionConstraintErrors.AUTHORIZATION_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Valid role update with new name and valid authorizations passes and populates outAuthorizations")
    void testValidUpdateWithAuths() {
        UUID roleId = UUID.randomUUID();
        roleRepo.persist(new Role(roleId, "OLD_ROLE", "old_desc", false));

        UUID authId = UUID.randomUUID();
        authRepo.storage.put(authId, new Authorization(authId, "AUTH_KEY", "Auth Name", "Desc", "Group"));

        RoleUpdateRequestDTO req = new RoleUpdateRequestDTO(roleId, "NEW_ROLE", "new_desc", List.of(authId));
        List<Authorization> outAuths = new ArrayList<>();

        Role res = constraint.checkConstraints(req, outAuths);
        assertNotNull(res);
        assertEquals(roleId, res.getId());
        assertEquals(1, outAuths.size());
        assertEquals(authId, outAuths.get(0).getId());
    }

    @Test
    @DisplayName("Updating with same name, null name, blank name, and null outAuthorizations passes")
    void testSameNameAndNullOutAuths() {
        UUID roleId = UUID.randomUUID();
        roleRepo.persist(new Role(roleId, "SAME_NAME", "desc", false));

        // Same name
        Role res1 = constraint.checkConstraints(new RoleUpdateRequestDTO(roleId, "SAME_NAME", "desc", null), null);
        assertNotNull(res1);

        // Null name
        Role res2 = constraint.checkConstraints(new RoleUpdateRequestDTO(roleId, null, "desc", null), null);
        assertNotNull(res2);

        // Blank name
        Role res3 = constraint.checkConstraints(new RoleUpdateRequestDTO(roleId, "   ", "desc", null), null);
        assertNotNull(res3);
    }

    @Test
    @DisplayName("Throws AUTHORIZATION_NOT_FOUND when null element is in authorizationIds list")
    void testNullElementInAuthorizationIds() {
        UUID roleId = UUID.randomUUID();
        roleRepo.persist(new Role(roleId, "ROLE_TEST", "desc", false));

        List<UUID> listWithNull = new ArrayList<>();
        listWithNull.add(null);

        RoleUpdateRequestDTO req = new RoleUpdateRequestDTO(roleId, "ROLE_TEST", "desc", listWithNull);
        List<Authorization> outAuths = new ArrayList<>();

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req, outAuths));
        assertEquals(PermissionConstraintErrors.AUTHORIZATION_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("Default 0-arg constructor instantiates successfully")
    void testDefaultConstructor() {
        RoleUpdateConstraint defaultCtor = new RoleUpdateConstraint();
        assertNotNull(defaultCtor);
    }
}
