package com.freelance.user.create;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.create.constraint.UserCreateConstraint;
import com.freelance.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateConstraint Tests")
class UserCreateConstraintTest {

    @Test
    @DisplayName("unique username and unique contact pass constraint check")
    void uniqueUsernameAndContact_passes() {
        FakeUserRepository userRepo = new FakeUserRepository();
        UserContactRepository contactRepo = new UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
                return Optional.empty();
            }
        };
        UserCreateConstraint constraint = new UserCreateConstraint(userRepo, contactRepo);
        assertDoesNotThrow(() -> constraint.checkConstraints(new UserCreateRequestDTO(
                "unique_user",
                "Pass1234",
                List.of(),
                List.of(new UserContactRequestDTO(AlertChannel.WHATSAPP, "+1234567890"))
        )));
    }

    @Test
    @DisplayName("existing username raises USER_ALREADY_EXISTS")
    void existingUsername_raisesError() {
        FakeUserRepository repo = new FakeUserRepository();
        repo.persist(new User("existing_user", "hashedPass"));

        UserCreateConstraint constraint = new UserCreateConstraint(repo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new UserCreateRequestDTO("existing_user", "Pass1234")));
        assertEquals(UserConstraintErrors.USER_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("existing contact for channel raises CONTACT_ALREADY_EXISTS")
    void existingContact_raisesError() {
        FakeUserRepository userRepo = new FakeUserRepository();
        UserContactRepository contactRepo = new UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
                if ("+1234567890".equals(contactValue) && channel == AlertChannel.WHATSAPP) {
                    return Optional.of(new UserContactEntity(UUID.randomUUID(), UUID.randomUUID(), channel, contactValue));
                }
                return Optional.empty();
            }
        };

        UserCreateConstraint constraint = new UserCreateConstraint(userRepo, contactRepo);
        UserCreateRequestDTO request = new UserCreateRequestDTO(
                "new_user",
                "Pass1234",
                List.of(),
                List.of(new UserContactRequestDTO(AlertChannel.WHATSAPP, "+1234567890"))
        );

        ConstraintException ex = assertThrows(ConstraintException.class, () -> constraint.checkConstraints(request));
        assertEquals(UserConstraintErrors.CONTACT_ALREADY_EXISTS, ex.getError());
    }

    @Test
    @DisplayName("Default constructor and null checks")
    void testConstructorsAndNullChecks() {
        UserCreateConstraint defaultConstraint = new UserCreateConstraint();
        assertNotNull(defaultConstraint);

        assertDoesNotThrow(() -> defaultConstraint.checkConstraints(null));
        assertDoesNotThrow(() -> defaultConstraint.checkConstraints(new UserCreateRequestDTO(null, "pass")));

        FakeUserRepository fakeRepo = new FakeUserRepository();
        UserCreateConstraint fakeConstraint = new UserCreateConstraint(fakeRepo, (UserContactRepository) null);
        assertDoesNotThrow(() -> fakeConstraint.checkConstraints(new UserCreateRequestDTO("user", "pass", List.of(), null)));
    }

    @Test
    @DisplayName("contacts with null item, null channel, or blank value are ignored in constraint check")
    void invalidContactItems_skippedInConstraintCheck() {
        FakeUserRepository userRepo = new FakeUserRepository();
        UserContactRepository contactRepo = new UserContactRepository() {
            @Override
            public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
                return Optional.empty();
            }
        };
        UserCreateConstraint constraint = new UserCreateConstraint(userRepo, contactRepo);

        List<UserContactRequestDTO> contacts = new java.util.ArrayList<>();
        contacts.add(null);
        contacts.add(new UserContactRequestDTO(null, "+1234567890"));
        contacts.add(new UserContactRequestDTO(AlertChannel.WHATSAPP, null));
        contacts.add(new UserContactRequestDTO(AlertChannel.WHATSAPP, "   "));
        contacts.add(new UserContactRequestDTO(AlertChannel.PUSH, "fcm_token_12345"));

        assertDoesNotThrow(() -> constraint.checkConstraints(new UserCreateRequestDTO(
                "new_user",
                "Pass1234",
                List.of(),
                contacts
        )));
    }
}
