package com.freelance.user.create;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.user.create.assembler.UserCreateAssembler;
import com.freelance.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    @DisplayName("createUser persists user and contact, returns create response")
    void createUser_persistsAndReturns() {
        FakeUserRepository repo = new FakeUserRepository();
        UserContactRepository contactRepo = new UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
                return Optional.empty();
            }
        };

        UserCreateService service = new UserCreateService(
                repo,
                new com.freelance.mapping.userRole.repository.UserRoleRepository(),
                contactRepo,
                new UserCreateAssembler()
        );
        UserCreateResponseDTO response = service.createUser(new UserCreateRequestDTO(
                "new_user",
                "Password123",
                AlertChannel.WHATSAPP,
                "+1234567890"
        ));

        assertNotNull(response);
        assertEquals("new_user", response.username());
        assertEquals(1, repo.persistCalls);
        assertTrue(repo.existsByUsername("new_user"));
        User storedUser = repo.get(response.id());
        assertNotNull(storedUser);
        assertEquals(1, storedUser.getContacts().size());
        assertEquals("+1234567890", storedUser.getContacts().get(0).getContactValue());
        assertEquals(AlertChannel.WHATSAPP, storedUser.getContacts().get(0).getChannel());
        assertEquals(1, response.contacts().size());
        assertEquals("+1234567890", response.contacts().get(0).contactValue());
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
        UserContactRepository contactRepo = new UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
                return Optional.empty();
            }

            @Override
            public void persist(UserContactEntity entity) {
            }
        };
        UserCreateService service = new UserCreateService(repo, roleRepo, contactRepo, new UserCreateAssembler());

        UUID roleId = UUID.randomUUID();
        UserCreateResponseDTO response = service.createUser(new UserCreateRequestDTO(
                "user_with_role",
                "Password123",
                List.of(roleId),
                AlertChannel.WHATSAPP,
                "+19876543210"
        ));

        assertNotNull(response);
        assertEquals("user_with_role", response.username());
        assertEquals(1, response.roleIds().size());
        assertEquals(1, persistCount[0]);
        assertEquals(1, response.contacts().size());
    }

    @Test
    @DisplayName("test all UserCreateService constructors")
    void testConstructors() {
        assertNotNull(new UserCreateService());
        assertNotNull(new UserCreateService(new FakeUserRepository(), new UserCreateAssembler()));
        assertNotNull(new UserCreateService(
                new FakeUserRepository(),
                new com.freelance.mapping.userRole.repository.UserRoleRepository(),
                new UserCreateAssembler()
        ));
    }

    @Test
    @DisplayName("createUser with null role element in roleIds list filters it out")
    void createUser_withNullRoleInList() {
        FakeUserRepository repo = new FakeUserRepository();
        UserContactRepository contactRepo = new UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
                return Optional.empty();
            }
            @Override
            public void persist(UserContactEntity entity) {}
        };
        com.freelance.mapping.userRole.repository.UserRoleRepository roleRepo = new com.freelance.mapping.userRole.repository.UserRoleRepository() {
            @Override
            public void persist(com.freelance.mapping.userRole.entity.UserRoleEntity entity) {}
        };
        UserCreateService service = new UserCreateService(repo, roleRepo, contactRepo, new UserCreateAssembler());

        List<UUID> rolesWithNull = new ArrayList<>();
        rolesWithNull.add(null);
        UUID validRoleId = UUID.randomUUID();
        rolesWithNull.add(validRoleId);

        UserCreateResponseDTO response = service.createUser(new UserCreateRequestDTO(
                "filtered_user",
                "Password123",
                rolesWithNull,
                List.of(new com.freelance.user.contact.UserContactRequestDTO(AlertChannel.WHATSAPP, "+1234567890"))
        ));

        assertNotNull(response);
        assertEquals(1, response.roleIds().size());
        assertEquals(validRoleId, response.roleIds().get(0));
    }

    @Test
    @DisplayName("createUser persists multiple distinct channels")
    void createUser_multipleContacts() {
        FakeUserRepository repo = new FakeUserRepository();
        UserContactRepository contactRepo = new UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
                return Optional.empty();
            }
        };
        UserCreateService service = new UserCreateService(
                repo,
                new com.freelance.mapping.userRole.repository.UserRoleRepository(),
                contactRepo,
                new UserCreateAssembler()
        );

        UserCreateResponseDTO response = service.createUser(new UserCreateRequestDTO(
                "multi_contact_user",
                "Password123",
                List.of(),
                List.of(
                        new com.freelance.user.contact.UserContactRequestDTO(AlertChannel.WHATSAPP, "+1234567890"),
                        new com.freelance.user.contact.UserContactRequestDTO(AlertChannel.PUSH, "fcm_token_12345")
                )
        ));

        assertNotNull(response);
        assertEquals(2, response.contacts().size());
        User storedUser = repo.get(response.id());
        assertNotNull(storedUser);
        assertEquals(2, storedUser.getContacts().size());
    }
}
