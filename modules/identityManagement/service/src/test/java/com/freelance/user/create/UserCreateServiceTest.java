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
}
