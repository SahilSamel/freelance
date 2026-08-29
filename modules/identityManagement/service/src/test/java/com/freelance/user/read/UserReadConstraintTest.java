package com.freelance.user.read;

import com.freelance.entity.user.User;
import com.freelance.keys.identityManagement.ConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.user.read.constraint.UserReadConstraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserReadConstraint Tests")
class UserReadConstraintTest {

    @Test
    @DisplayName("existing user passes constraint check")
    void existingUser_passes() {
        User user = new User("user1", "hash1");
        assertDoesNotThrow(() -> new UserReadConstraint().checkUserExists(user));
    }

    @Test
    @DisplayName("null user raises USER_NOT_FOUND")
    void nullUser_raisesError() {
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> new UserReadConstraint().checkUserExists(null));
        assertEquals(ConstraintErrors.USER_NOT_FOUND, ex.getError());
    }
}
