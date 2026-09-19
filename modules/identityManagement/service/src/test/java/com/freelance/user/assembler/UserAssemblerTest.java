package com.freelance.user.assembler;

import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.create.UserCreateResponseDTO;
import com.freelance.user.create.assembler.UserCreateAssembler;
import com.freelance.user.entity.User;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;
import com.freelance.user.read.assembler.UserReadAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateAssembler & UserReadAssembler Unit Tests")
class UserAssemblerTest {

    private final UserCreateAssembler createAssembler = new UserCreateAssembler();
    private final UserReadAssembler readAssembler = new UserReadAssembler();

    @Test
    @DisplayName("UserCreateAssembler converts create request to entity and entity to response DTO")
    void testUserCreateAssembler() {
        assertNull(createAssembler.toEntity(null));
        assertNull(createAssembler.fromEntity(null));
        assertNull(createAssembler.fromEntity(null, List.of()));

        UserCreateRequestDTO request = new UserCreateRequestDTO("testuser", "Password@123");
        User user = createAssembler.toEntity(request);
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertNotNull(user.getPassword());

        UUID userId = UUID.randomUUID();
        user.setId(userId);

        UserCreateResponseDTO response1 = createAssembler.fromEntity(user);
        assertNotNull(response1);
        assertEquals(userId, response1.id());
        assertEquals("testuser", response1.username());

        UUID roleId = UUID.randomUUID();
        UserCreateResponseDTO response2 = createAssembler.fromEntity(user, List.of(roleId));
        assertNotNull(response2);
        assertEquals(userId, response2.id());
        assertEquals(1, response2.roleIds().size());
        assertEquals(roleId, response2.roleIds().get(0));

        UserCreateResponseDTO response3 = createAssembler.fromEntity(user, null);
        assertNotNull(response3);
        assertTrue(response3.roleIds().isEmpty());
    }

    @Test
    @DisplayName("UserReadAssembler converts read request to entity and entity to response DTO")
    void testUserReadAssembler() {
        assertNull(readAssembler.toEntity(null));
        assertNull(readAssembler.fromEntity(null));

        UUID userId = UUID.randomUUID();
        UserReadRequestDTO request = new UserReadRequestDTO(userId, "readuser");
        User user = readAssembler.toEntity(request);
        assertNotNull(user);
        assertEquals(userId, user.getId());
        assertEquals("readuser", user.getUsername());

        LocalDateTime now = LocalDateTime.now();
        user.setPassword("hashedPass");
        user.setCreatedDate(now);
        user.setLastUpdatedDate(now);

        UserReadResponseDTO response = readAssembler.fromEntity(user);
        assertNotNull(response);
        assertNotNull(response.userDTO());
        assertEquals(userId, response.userDTO().id());
        assertEquals("readuser", response.userDTO().username());
        assertEquals("hashedPass", response.userDTO().password());
        assertEquals(now, response.userDTO().createdDate());
        assertEquals(now, response.userDTO().lastUpdatedDate());
    }
}
