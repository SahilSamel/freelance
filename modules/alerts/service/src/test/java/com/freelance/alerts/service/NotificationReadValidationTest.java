package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.alerts.validation.NotificationReadValidation;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationReadValidationTest {

    private TransactionContext context;
    private NotificationReadValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new NotificationReadValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should throw ValidationException when request is null")
    void testNullRequest() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().containsKey("userId"));
        assertTrue(context.getValidationErrors().get("userId").contains(AlertEmitValidationErrors.INVALID_USER_IDS));
    }

    @Test
    @DisplayName("Should throw ValidationException when userId is null")
    void testNullUserId() {
        NotificationReadRequestDTO request = new NotificationReadRequestDTO(null, List.of(UUID.randomUUID()), AlertChannel.PUSH);

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("userId"));
        assertTrue(context.getValidationErrors().get("userId").contains(AlertEmitValidationErrors.INVALID_USER_IDS));
    }

    @Test
    @DisplayName("Should pass validation when userId is provided")
    void testValidRequest() {
        NotificationReadRequestDTO request = new NotificationReadRequestDTO(UUID.randomUUID(), List.of(UUID.randomUUID()), AlertChannel.PUSH);

        assertDoesNotThrow(() -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().isEmpty());
    }
}
