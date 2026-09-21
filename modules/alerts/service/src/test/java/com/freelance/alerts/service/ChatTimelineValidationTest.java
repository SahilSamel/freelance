package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.ChatTimelineRequestDTO;
import com.freelance.chat.validation.ChatTimelineValidation;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ChatTimelineValidationTest {

    private TransactionContext context;
    private ChatTimelineValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new ChatTimelineValidation();
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
        assertTrue(context.getValidationErrors().containsKey("channel"));
    }

    @Test
    @DisplayName("Should throw ValidationException when userId is null")
    void testNullUserId() {
        ChatTimelineRequestDTO request = new ChatTimelineRequestDTO(null, AlertChannel.WHATSAPP, 50);

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("userId"));
        assertTrue(context.getValidationErrors().get("userId").contains(AlertEmitValidationErrors.INVALID_USER_IDS));
    }

    @Test
    @DisplayName("Should throw ValidationException when channel is null")
    void testNullChannel() {
        ChatTimelineRequestDTO request = new ChatTimelineRequestDTO(UUID.randomUUID(), null, 50);

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("channel"));
        assertTrue(context.getValidationErrors().get("channel").contains(AlertEmitValidationErrors.INVALID_CHANNEL));
    }

    @Test
    @DisplayName("Should pass validation when userId and channel are provided")
    void testValidRequest() {
        ChatTimelineRequestDTO request = new ChatTimelineRequestDTO(UUID.randomUUID(), AlertChannel.WHATSAPP, 50);

        assertDoesNotThrow(() -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().isEmpty());
    }
}
