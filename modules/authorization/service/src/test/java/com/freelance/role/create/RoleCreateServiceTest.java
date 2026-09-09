package com.freelance.role.create;

import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.authorization.dto.role.create.RoleCreateResponseDTO;
import com.freelance.authorization.entity.Authorization;
import com.freelance.fixtures.FakeAuthorizationRepository;
import com.freelance.fixtures.FakeRoleAuthorizationRepository;
import com.freelance.fixtures.FakeRoleRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.role.assembler.RoleAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RoleCreateService Tests")
class RoleCreateServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("createRole forces is_system = false and persists role and authorization mappings")
    void createRole_forcesNonSystemAndPersists() {
        FakeRoleRepository roleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository roleAuthRepo = new FakeRoleAuthorizationRepository();
        FakeAuthorizationRepository authRepo = new FakeAuthorizationRepository();

        UUID authId = UUID.randomUUID();
        authRepo.storage.put(authId, new Authorization(authId, "AUTH_KEY", "Auth Name", "Desc", "Group1"));

        RoleCreateService service = new RoleCreateService(roleRepo, roleAuthRepo, authRepo, new RoleAssembler());
        RoleCreateResponseDTO response = service.createRole(
                new RoleCreateRequestDTO("CUSTOM_ROLE", "Custom role desc", List.of(authId))
        );

        assertNotNull(response);
        assertEquals("CUSTOM_ROLE", response.name());
        assertFalse(response.isSystem(), "User-created roles must always have isSystem = false");
        assertEquals(1, response.authorizations().size());
        assertEquals(1, roleRepo.persistCalls);
        assertEquals(1, roleAuthRepo.persistBatchCalls);
    }
}
