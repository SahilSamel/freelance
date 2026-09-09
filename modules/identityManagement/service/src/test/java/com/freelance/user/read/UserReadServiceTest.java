package com.freelance.user.read;

import com.freelance.user.entity.User;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.user.read.assembler.UserReadAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserReadService Tests")
class UserReadServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("readUser by username returns response DTO")
    void readUserByUsername_returnsResponse() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User entity = new User(userId, "john_doe", "hashedPass");
        repo.persist(entity);

        UserReadService service = new UserReadService(repo, new UserReadAssembler());
        UserReadResponseDTO response = service.readUser(new UserReadRequestDTO(null, "john_doe"));

        assertNotNull(response);
        assertNotNull(response.userDTO());
        assertEquals("john_doe", response.userDTO().username());
        assertEquals(userId, response.userDTO().id());
    }

    @Test
    @DisplayName("readUser by id returns response DTO")
    void readUserById_returnsResponse() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User entity = new User(userId, "john_doe", "hashedPass");
        repo.persist(entity);

        UserReadService service = new UserReadService(repo, new UserReadAssembler());
        UserReadResponseDTO response = service.readUser(new UserReadRequestDTO(userId, null));

        assertNotNull(response);
        assertNotNull(response.userDTO());
        assertEquals("john_doe", response.userDTO().username());
        assertEquals(userId, response.userDTO().id());
    }
}
