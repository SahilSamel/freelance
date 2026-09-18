package com.freelance.user.create;

import com.freelance.user.entity.User;
import com.freelance.fixtures.FakeUserRepository;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.user.create.constraint.UserCreateConstraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateConstraint Tests")
class UserCreateConstraintTest {

    @Test
    @DisplayName("unique username passes constraint check")
    void uniqueUsername_passes() {
        FakeUserRepository repo = new FakeUserRepository();
        UserCreateConstraint constraint = new UserCreateConstraint(repo);
        assertDoesNotThrow(() -> constraint.checkConstraints(new UserCreateRequestDTO("unique_user", "Pass1234")));
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
}
