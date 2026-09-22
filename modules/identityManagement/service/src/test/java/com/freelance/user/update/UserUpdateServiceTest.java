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

    @Test
    @DisplayName("updateUser with contacts updates user contacts and returns them in response DTO")
    void updateUser_withContacts() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "old_name", "oldHash");
        repo.persist(existing);

        com.freelance.alerts.preference.repository.UserContactRepository contactRepo = new com.freelance.alerts.preference.repository.UserContactRepository() {
            @Override
            public java.util.Optional<com.freelance.alerts.preference.entity.UserContactEntity> findByContactAndChannel(String contactValue, com.freelance.alerts.AlertChannel channel) {
                return java.util.Optional.empty();
            }
        };

        UserUpdateService service = new UserUpdateService(repo, contactRepo, new UserUpdateAssembler());
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(
                userId,
                "updated_name",
                "NewPassword123",
                com.freelance.alerts.AlertChannel.WHATSAPP,
                "+19876543210"
        );
        UserUpdateResponseDTO response = service.updateUser(request);

        assertNotNull(response);
        assertEquals("updated_name", response.username());
        assertEquals(userId, response.id());
        assertEquals(1, response.contacts().size());
        assertEquals(com.freelance.alerts.AlertChannel.WHATSAPP, response.contacts().get(0).channel());
        assertEquals("+19876543210", response.contacts().get(0).contactValue());
        assertEquals(1, repo.mergeCalls);
    }

    @Test
    @DisplayName("test UserUpdateService constructors")
    void testConstructors() {
        assertNotNull(new UserUpdateService());
        assertNotNull(new UserUpdateService(new FakeUserRepository(), new UserUpdateAssembler()));
        assertNotNull(new UserUpdateService(
                new FakeUserRepository(),
                new com.freelance.alerts.preference.repository.UserContactRepository(),
                new UserUpdateAssembler()
        ));
    }
}
