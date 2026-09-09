package com.freelance.user.update;

import com.freelance.user.entity.User;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.keys.identityManagement.ConstraintErrors;
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
        assertEquals(ConstraintErrors.USER_TO_UPDATE_NOT_FOUND, ex.getError());
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
        assertEquals(ConstraintErrors.USER_ALREADY_EXISTS, ex.getError());
    }
}
