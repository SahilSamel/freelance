package com.freelance.authorization.service.list;

import com.freelance.authorization.dto.AuthorizationDTO;
import com.freelance.authorization.dto.list.AuthorizationListResponseDTO;
import com.freelance.authorization.service.list.assembler.AuthorizationListAssembler;
import com.freelance.entity.authorization.Authorization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthorizationListAssembler Tests")
class AuthorizationListAssemblerTest {

    private final AuthorizationListAssembler assembler = new AuthorizationListAssembler();

    @Test
    @DisplayName("toDTO maps entity attributes correctly")
    void toDTO_mapsCorrectly() {
        UUID id = UUID.randomUUID();
        Authorization entity = new Authorization(id, "USER_CREATE", "Create User", "Creates user", "IdentityManagement");

        AuthorizationDTO dto = assembler.toDTO(entity);
        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("USER_CREATE", dto.key());
        assertEquals("Create User", dto.name());
        assertEquals("Creates user", dto.description());
        assertEquals("IdentityManagement", dto.groupPath());
    }

    @Test
    @DisplayName("toDTO returns null for null entity")
    void toDTO_returnsNullForNull() {
        assertNull(assembler.toDTO(null));
    }

    @Test
    @DisplayName("toResponseDTO handles empty and populated lists with hierarchical grouping")
    void toResponseDTO_handlesLists() {
        AuthorizationListResponseDTO emptyResponse = assembler.toResponseDTO(Collections.emptyList());
        assertNotNull(emptyResponse);
        assertEquals(0, emptyResponse.totalCount());
        assertTrue(emptyResponse.groups().isEmpty());

        Authorization a1 = new Authorization(UUID.randomUUID(), "KEY1", "Name1", "Desc1", "Security.Management");
        Authorization a2 = new Authorization(UUID.randomUUID(), "KEY2", "Name2", "Desc2", "Security.Management");
        Authorization a3 = new Authorization(UUID.randomUUID(), "KEY3", "Name3", "Desc3", "Identity.User");

        AuthorizationListResponseDTO populated = assembler.toResponseDTO(List.of(a1, a2, a3));
        assertNotNull(populated);
        assertEquals(3, populated.totalCount());
        assertEquals(2, populated.groups().size()); // Security and Identity

        // Security -> Management -> [KEY1, KEY2]
        assertEquals("Security", populated.groups().get(0).name());
        assertEquals(1, populated.groups().get(0).subGroups().size());
        assertEquals("Management", populated.groups().get(0).subGroups().get(0).name());
        assertEquals(2, populated.groups().get(0).subGroups().get(0).authorizations().size());

        // Identity -> User -> [KEY3]
        assertEquals("Identity", populated.groups().get(1).name());
        assertEquals(1, populated.groups().get(1).subGroups().size());
        assertEquals("User", populated.groups().get(1).subGroups().get(0).name());
        assertEquals(1, populated.groups().get(1).subGroups().get(0).authorizations().size());
    }
}
