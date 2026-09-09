package com.freelance.user.update;

import com.freelance.user.entity.User;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.user.update.assembler.UserUpdateAssembler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserUpdateService Tests")
class UserUpdateServiceTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("updateUser updates user fields and returns response DTO")
    void updateUser_success() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "old_name", "oldHash");
        repo.persist(existing);

        UserUpdateService service = new UserUpdateService(repo, new UserUpdateAssembler());
        UserUpdateResponseDTO response = service.updateUser(new UserUpdateRequestDTO(userId, "new_name", "NewPassword123"));

        assertNotNull(response);
        assertEquals("new_name", response.username());
        assertEquals(userId, response.id());
        assertEquals(1, repo.mergeCalls);
    }
}
