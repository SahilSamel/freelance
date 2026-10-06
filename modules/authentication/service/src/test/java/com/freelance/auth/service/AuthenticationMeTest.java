package com.freelance.auth.service;

import com.freelance.auth.service.constraint.MeConstraint;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.user.read.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("Authentication MeConstraint Unit Tests")
class AuthenticationMeTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("MeConstraint throws INVALID_SESSION when userId is null")
    void testMe_nullUserId_throwsConstraintException() {
        Transaction.getRequestContext().withUserId(null);
        MeConstraint constraint = new MeConstraint();
        assertThrows(ConstraintException.class, constraint::checkConstraints);
    }

    @Test
    @DisplayName("MeConstraint throws INVALID_SESSION when user does not exist in repository")
    void testMe_nonExistentUser_throwsConstraintException() {
        UUID testUserId = UUID.randomUUID();
        Transaction.getRequestContext().withUserId(testUserId);

        User mockUserService = Mockito.mock(User.class);
        when(mockUserService.existsById(testUserId)).thenReturn(false);

        MeConstraint constraint = new MeConstraint(mockUserService);
        assertThrows(ConstraintException.class, constraint::checkConstraints);
    }

    @Test
    @DisplayName("MeConstraint passes when user exists in repository")
    void testMe_existingUser_passes() {
        UUID testUserId = UUID.randomUUID();
        Transaction.getRequestContext().withUserId(testUserId);

        User mockUserService = Mockito.mock(User.class);
        when(mockUserService.existsById(testUserId)).thenReturn(true);

        MeConstraint constraint = new MeConstraint(mockUserService);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(constraint::checkConstraints);
    }
}
