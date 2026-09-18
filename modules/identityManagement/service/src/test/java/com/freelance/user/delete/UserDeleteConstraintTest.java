package com.freelance.user.delete;

import com.freelance.user.entity.User;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.user.delete.constraint.UserDeleteConstraint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserDeleteConstraint Tests")
class UserDeleteConstraintTest {

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
    @DisplayName("existing other user passes constraint check")
    void validDelete_passes() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID targetId = UUID.randomUUID();
        User existing = new User(targetId, "user_to_delete", "hash");
        repo.persist(existing);

        UserDeleteConstraint constraint = new UserDeleteConstraint(repo);
        User result = constraint.checkConstraints(new UserDeleteRequestDTO(targetId));
        assertNotNull(result);
        assertEquals(targetId, result.getId());
    }

    @Test
    @DisplayName("unknown user raises USER_TO_DELETE_NOT_FOUND")
    void unknownUser_raisesError() {
        FakeUserRepository repo = new FakeUserRepository();
        UUID targetId = UUID.randomUUID();

        UserDeleteConstraint constraint = new UserDeleteConstraint(repo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new UserDeleteRequestDTO(targetId)));
        assertEquals(UserConstraintErrors.USER_TO_DELETE_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("attempting to delete own account raises CANNOT_DELETE_SELF")
    void deleteSelf_raisesError() {
        FakeUserRepository repo = new FakeUserRepository();
        User selfUser = new User(callerId, "current_user", "hash");
        repo.persist(selfUser);

        UserDeleteConstraint constraint = new UserDeleteConstraint(repo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> constraint.checkConstraints(new UserDeleteRequestDTO(callerId)));
        assertEquals(UserConstraintErrors.CANNOT_DELETE_SELF, ex.getError());
    }
}
