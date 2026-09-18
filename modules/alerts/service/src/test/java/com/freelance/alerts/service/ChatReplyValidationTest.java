package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.AdminChatReplyRequestDTO;
import com.freelance.chat.validation.ChatReplyValidation;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.chat.ChatValidationErrors;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ChatReplyValidationTest {

    private TransactionContext context;
    private ChatReplyValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new ChatReplyValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should throw ValidationException when userId is null")
    void testNullUserId() {
        AdminChatReplyRequestDTO request = new AdminChatReplyRequestDTO(null, AlertChannel.WHATSAPP, "Hello");

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("userId"));
        assertTrue(context.getValidationErrors().get("userId").contains(AlertEmitValidationErrors.INVALID_USER_IDS));
    }

    @Test
    @DisplayName("Should throw ValidationException when channel is null")
    void testNullChannel() {
        AdminChatReplyRequestDTO request = new AdminChatReplyRequestDTO(UUID.randomUUID(), null, "Hello");

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("channel"));
        assertTrue(context.getValidationErrors().get("channel").contains(AlertEmitValidationErrors.INVALID_CHANNEL));
    }

    @Test
    @DisplayName("Should throw ValidationException when message is blank")
    void testBlankMessage() {
        AdminChatReplyRequestDTO request = new AdminChatReplyRequestDTO(UUID.randomUUID(), AlertChannel.WHATSAPP, "   ");

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("message"));
        assertTrue(context.getValidationErrors().get("message").contains(ChatValidationErrors.INVALID_CHAT_REPLY));
    }

    @Test
    @DisplayName("Should pass validation when all fields including channel are valid")
    void testValidRequest() {
        AdminChatReplyRequestDTO request = new AdminChatReplyRequestDTO(UUID.randomUUID(), AlertChannel.WHATSAPP, "Your request is in progress.");

        assertDoesNotThrow(() -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().isEmpty());
    }
}