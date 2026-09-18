package com.freelance.alerts.validation;

import com.freelance.alerts.ButtonType;
import com.freelance.alerts.dto.TemplateButtonDTO;
import com.freelance.alerts.dto.TemplateUpdateRequestDTO;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.template.TemplateValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.template.validation.TemplateUpdateValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TemplateUpdateValidationTest {

    private TransactionContext context;
    private TemplateUpdateValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new TemplateUpdateValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should throw ValidationException when request is null")
    void testNullRequest() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().containsKey("id"));
        assertTrue(context.getValidationErrors().get("id").contains(TemplateValidationErrors.INVALID_TEMPLATE_ID));
    }

    @Test
    @DisplayName("Should throw ValidationException when both id and templateKey are missing/blank")
    void testMissingIdAndKeyThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                (UUID) null, "   ", "New Name", "UTILITY", "en", "Content", null, null, null, null
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("id"));
        assertTrue(context.getValidationErrors().get("id").contains(TemplateValidationErrors.INVALID_TEMPLATE_ID));
    }

    @Test
    @DisplayName("Should throw ValidationException when content is provided but blank")
    void testBlankContentThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "New Name", "UTILITY", "en", "   ", null, null, null, null
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("content"));
        assertTrue(context.getValidationErrors().get("content").contains(TemplateValidationErrors.INVALID_TEMPLATE_CONTENT));
    }

    @Test
    @DisplayName("Should pass validation for valid minimal update by ID or key")
    void testValidMinimalUpdatePasses() {
        TemplateUpdateRequestDTO reqById = new TemplateUpdateRequestDTO(UUID.randomUUID(), "Updated content");
        assertDoesNotThrow(() -> validation.validateRequest(reqById));

        TemplateUpdateRequestDTO reqByKey = new TemplateUpdateRequestDTO("my_template", "Updated content");
        assertDoesNotThrow(() -> validation.validateRequest(reqByKey));
    }

    @Test
    @DisplayName("Should throw ValidationException when more than 10 buttons are provided")
    void testTooManyButtonsThrows() {
        List<TemplateButtonDTO> elevenButtons = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            elevenButtons.add(new TemplateButtonDTO("B" + i, ButtonType.URL, "https://" + i + ".com", null, false, i));
        }
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, null, "Content", null, null, null, elevenButtons
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons"));
        assertTrue(context.getValidationErrors().get("buttons").contains(TemplateValidationErrors.TOO_MANY_BUTTONS));
    }

    @Test
    @DisplayName("Should throw ValidationException on duplicate button texts")
    void testDuplicateButtonTextThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, null, "Content", null, null, null,
                List.of(
                        new TemplateButtonDTO("Option", ButtonType.URL, "https://1.com", null, false, 1),
                        new TemplateButtonDTO("option", ButtonType.URL, "https://2.com", null, false, 2)
                )
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons.text"));
        assertTrue(context.getValidationErrors().get("buttons.text").contains(TemplateValidationErrors.DUPLICATE_BUTTON_TEXT));
    }

    @Test
    @DisplayName("Should throw ValidationException on duplicate button sequence orders")
    void testDuplicateButtonSequenceOrderThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, null, "Content", null, null, null,
                List.of(
                        new TemplateButtonDTO("Option 1", ButtonType.URL, "https://1.com", null, false, 1),
                        new TemplateButtonDTO("Option 2", ButtonType.URL, "https://2.com", null, false, 1)
                )
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons.sequenceOrder"));
        assertTrue(context.getValidationErrors().get("buttons.sequenceOrder").contains(TemplateValidationErrors.DUPLICATE_BUTTON_SEQUENCE));
    }

    @Test
    @DisplayName("Should throw ValidationException on duplicate button payloads")
    void testDuplicateButtonPayloadThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, null, "Content", null, null, null,
                List.of(
                        new TemplateButtonDTO("Option 1", ButtonType.QUICK_REPLY, "SAME_PAYLOAD", null, true, 1),
                        new TemplateButtonDTO("Option 2", ButtonType.QUICK_REPLY, "SAME_PAYLOAD", null, true, 2)
                )
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons.payload"));
        assertTrue(context.getValidationErrors().get("buttons.payload").contains(TemplateValidationErrors.DUPLICATE_BUTTON_PAYLOAD));
    }

    @Test
    @DisplayName("Should throw ValidationException when non-terminal button has no replyTemplateKey")
    void testNonTerminalButtonWithoutReplyKeyThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, null, "Content", null, null, null,
                List.of(
                        new TemplateButtonDTO("Option 1", ButtonType.QUICK_REPLY, "PAYLOAD_1", null, false, 1)
                )
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("buttons.replyTemplateKey"));
        assertTrue(context.getValidationErrors().get("buttons.replyTemplateKey").contains(TemplateValidationErrors.INVALID_TEMPLATE_BUTTON));
    }

    @Test
    @DisplayName("Should pass validation when updating priority with HIGH, NORMAL, LOW or null/blank")
    void testValidPriorityPasses() {
        TemplateUpdateRequestDTO reqHigh = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, "HIGH", null, "Content", null, null, null, null
        );
        TemplateUpdateRequestDTO reqNull = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, null, null, "Content", null, null, null, null
        );

        assertDoesNotThrow(() -> validation.validateRequest(reqHigh));
        assertDoesNotThrow(() -> validation.validateRequest(reqNull));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("Should throw ValidationException when updating priority with invalid value")
    void testInvalidPriorityThrows() {
        TemplateUpdateRequestDTO req = new TemplateUpdateRequestDTO(
                UUID.randomUUID(), null, "Name", null, "INVALID_PRIORITY", null, "Content", null, null, null, null
        );

        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().containsKey("priority"));
        assertTrue(context.getValidationErrors().get("priority").contains(TemplateValidationErrors.INVALID_TEMPLATE_PRIORITY));
    }
}
