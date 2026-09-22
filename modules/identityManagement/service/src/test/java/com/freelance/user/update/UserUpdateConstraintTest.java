package com.freelance.user.update;

import com.freelance.user.entity.User;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.user.update.constraint.UserUpdateConstraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserUpdateConstraint Tests")
class UserUpdateConstraintTest {

    @Test
    @DisplayName("existing user and unique new username passes constraint check")
    void validUpdate_passes() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "old_name", "hashedPass");
        repo.persist(existing);

        UserUpdateConstraint constraint = new UserUpdateConstraint(repo);
        User result = constraint.checkConstraints(new UserUpdateRequestDTO(userId, "new_name", null));
        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    @DisplayName("unknown user id raises USER_TO_UPDATE_NOT_FOUND")
    void unknownUserId_raisesError() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();

        UserUpdateConstraint constraint = new UserUpdateConstraint(repo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new UserUpdateRequestDTO(userId, "new_name", null)));
        assertEquals(UserConstraintErrors.USER_TO_UPDATE_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("duplicate username owned by another user raises USER_ALREADY_EXISTS")
    void duplicateUsername_raisesError() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User existing = new User(userId, "old_name", "hashedPass");
        User otherUser = new User(otherUserId, "taken_name", "otherHash");
        repo.persist(existing);
        repo.persist(otherUser);

        UserUpdateConstraint constraint = new UserUpdateConstraint(repo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new UserUpdateRequestDTO(userId, "taken_name", null)));
        assertEquals(UserConstraintErrors.USER_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("null request or null id returns null gracefully")
    void nullRequest_returnsNull() {
        FakeUserRepository repo = new FakeUserRepository();
        UserUpdateConstraint constraint = new UserUpdateConstraint(repo);
        assertNull(constraint.checkConstraints(null));
        assertNull(constraint.checkConstraints(new UserUpdateRequestDTO(null, "name", null)));
    }

    @Test
    @DisplayName("null or blank username or same username skips duplicate check and passes")
    void sameOrBlankUsername_passes() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "same_name", "hashedPass");
        repo.persist(existing);

        UserUpdateConstraint constraint = new UserUpdateConstraint(repo);

        // Same username
        User res1 = constraint.checkConstraints(new UserUpdateRequestDTO(userId, "same_name", "newPass"));
        assertNotNull(res1);

        // Null username
        User res2 = constraint.checkConstraints(new UserUpdateRequestDTO(userId, null, "newPass"));
        assertNotNull(res2);

        // Blank username
        User res3 = constraint.checkConstraints(new UserUpdateRequestDTO(userId, "   ", "newPass"));
        assertNotNull(res3);
    }

    @Test
    @DisplayName("default 0-arg constructor instantiates successfully")
    void defaultConstructor() {
        UserUpdateConstraint c = new UserUpdateConstraint();
        assertNotNull(c);
        assertNull(c.checkConstraints(null));
    }

    @Test
    @DisplayName("updating contact with existing contact of same user passes")
    void sameUserContact_passes() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "user1", "hashedPass");
        repo.persist(existing);

        com.freelance.alerts.preference.repository.UserContactRepository contactRepo = new com.freelance.alerts.preference.repository.UserContactRepository() {
            @Override
            public java.util.Optional<com.freelance.alerts.preference.entity.UserContactEntity> findByContactAndChannel(String contactValue, com.freelance.alerts.AlertChannel channel) {
                if ("+1234567890".equals(contactValue)) {
                    return java.util.Optional.of(new com.freelance.alerts.preference.entity.UserContactEntity(UUID.randomUUID(), userId, channel, contactValue));
                }
                return java.util.Optional.empty();
            }
        };

        UserUpdateConstraint constraint = new UserUpdateConstraint(repo, contactRepo);
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(
                userId,
                "user1",
                null,
                com.freelance.alerts.AlertChannel.WHATSAPP,
                "+1234567890"
        );
        User result = constraint.checkConstraints(request);
        assertNotNull(result);
    }

    @Test
    @DisplayName("updating contact with contact of another user raises CONTACT_ALREADY_EXISTS")
    void otherUserContact_raisesError() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User existing = new User(userId, "user1", "hashedPass");
        repo.persist(existing);

        com.freelance.alerts.preference.repository.UserContactRepository contactRepo = new com.freelance.alerts.preference.repository.UserContactRepository() {
            @Override
            public java.util.Optional<com.freelance.alerts.preference.entity.UserContactEntity> findByContactAndChannel(String contactValue, com.freelance.alerts.AlertChannel channel) {
                if ("+1234567890".equals(contactValue)) {
                    return java.util.Optional.of(new com.freelance.alerts.preference.entity.UserContactEntity(UUID.randomUUID(), otherUserId, channel, contactValue));
                }
                return java.util.Optional.empty();
            }
        };

        UserUpdateConstraint constraint = new UserUpdateConstraint(repo, contactRepo);
        UserUpdateRequestDTO request = new UserUpdateRequestDTO(
                userId,
                "user1",
                null,
                com.freelance.alerts.AlertChannel.WHATSAPP,
                "+1234567890"
        );
        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(request));
        assertEquals(UserConstraintErrors.CONTACT_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("null contact items or blank contact values in update are safely skipped")
    void invalidContactItems_skipped() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID userId = UUID.randomUUID();
        User existing = new User(userId, "user1", "hashedPass");
        repo.persist(existing);

        com.freelance.alerts.preference.repository.UserContactRepository contactRepo = new com.freelance.alerts.preference.repository.UserContactRepository() {
            @Override
            public java.util.Optional<com.freelance.alerts.preference.entity.UserContactEntity> findByContactAndChannel(String contactValue, com.freelance.alerts.AlertChannel channel) {
                return java.util.Optional.empty();
            }
        };

        UserUpdateConstraint constraint = new UserUpdateConstraint(repo, contactRepo);
        java.util.List<com.freelance.user.contact.UserContactRequestDTO> contacts = new java.util.ArrayList<>();
        contacts.add(null);
        contacts.add(new com.freelance.user.contact.UserContactRequestDTO(null, "+1234567890"));
        contacts.add(new com.freelance.user.contact.UserContactRequestDTO(com.freelance.alerts.AlertChannel.WHATSAPP, null));
        contacts.add(new com.freelance.user.contact.UserContactRequestDTO(com.freelance.alerts.AlertChannel.WHATSAPP, "   "));
        contacts.add(new com.freelance.user.contact.UserContactRequestDTO(com.freelance.alerts.AlertChannel.PUSH, "token123"));

        UserUpdateRequestDTO request = new UserUpdateRequestDTO(userId, "user1", null, contacts);
        assertDoesNotThrow(() -> constraint.checkConstraints(request));
    }
}
