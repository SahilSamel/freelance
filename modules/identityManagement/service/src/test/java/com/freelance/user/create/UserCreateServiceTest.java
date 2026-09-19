package com.freelance.user.create;

import com.freelance.fixtures.FakeUserRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.user.create.assembler.UserCreateAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateService Tests")
class UserCreateServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("createUser persists user and returns create response")
    void createUser_persistsAndReturns() {
        FakeUserRepository repo = new FakeUserRepository();
        UserCreateService service = new UserCreateService(repo, new UserCreateAssembler());
        UserCreateResponseDTO response = service.createUser(new UserCreateRequestDTO("new_user", "Password123"));

        assertNotNull(response);
        assertEquals("new_user", response.username());
        assertEquals(1, repo.persistCalls);
        assertTrue(repo.existsByUsername("new_user"));
    }

    @Test
    @DisplayName("createUser with roleIds assigns roles")
    void createUser_withRoleIds() {
        FakeUserRepository repo = new FakeUserRepository();
        final int[] persistCount = {0};
        com.freelance.mapping.userRole.repository.UserRoleRepository roleRepo = new com.freelance.mapping.userRole.repository.UserRoleRepository() {
            @Override
            public void persist(com.freelance.mapping.userRole.entity.UserRoleEntity entity) {
                persistCount[0]++;
            }
        };
        UserCreateService service = new UserCreateService(repo, roleRepo, new UserCreateAssembler());

        java.util.UUID roleId = java.util.UUID.randomUUID();
        UserCreateResponseDTO response = service.createUser(new UserCreateRequestDTO("user_with_role", "Password123", java.util.List.of(roleId)));

        assertNotNull(response);
        assertEquals("user_with_role", response.username());
        assertEquals(1, response.roleIds().size());
        assertEquals(1, persistCount[0]);
    }
}

