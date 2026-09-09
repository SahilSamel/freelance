package com.freelance.user.delete;

import com.freelance.user.entity.User;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.fixtures.TestContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserDeleteService Tests")
class UserDeleteServiceTest {

    private UUID callerId;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        TestContextHelper.setUp(callerId);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("deleteUser removes entity and returns response DTO")
    void deleteUser_success() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID targetId = UUID.randomUUID();
        User existing = new User(targetId, "user_to_delete", "hash");
        repo.persist(existing);

        UserDeleteService service = new UserDeleteService(repo);
        UserDeleteResponseDTO response = service.deleteUser(new UserDeleteRequestDTO(targetId));

        assertNotNull(response);
        assertEquals(targetId, response.id());
        assertTrue(response.deleted());
        assertEquals(1, repo.removeCalls);
        assertFalse(repo.findById(targetId).isPresent());
    }
}
