package com.freelance.auth.support.validation;

import com.freelance.auth.dto.SupportRequestDTO;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.authentication.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SupportRequestValidation Tests")
class SupportRequestValidationTest {

    private TransactionContext context;
    private SupportRequestValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new SupportRequestValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid request passes validation")
    void validRequest_passes() {
        SupportRequestDTO request = new SupportRequestDTO("Investigate performance regression", "TICKET-101");
        assertDoesNotThrow(() -> validation.validateRequest(request));
    }

    @Test
    @DisplayName("null request raises INVALID_SUPPORT_REQUEST")
    void nullRequest_raisesError() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().get("request").contains(ValidationErrors.INVALID_SUPPORT_REQUEST));
    }

    @Test
    @DisplayName("null or blank reason raises INVALID_SUPPORT_REASON")
    void blankReason_raisesError() {
        SupportRequestDTO requestNullReason = new SupportRequestDTO(null, "TICKET-101");
        assertThrows(ValidationException.class, () -> validation.validateRequest(requestNullReason));
        assertTrue(context.getValidationErrors().get("reason").contains(ValidationErrors.INVALID_SUPPORT_REASON));

        TestContextHelper.tearDown();
        context = TestContextHelper.setUp();

        SupportRequestDTO requestBlankReason = new SupportRequestDTO("   ", "TICKET-101");
        assertThrows(ValidationException.class, () -> validation.validateRequest(requestBlankReason));
        assertTrue(context.getValidationErrors().get("reason").contains(ValidationErrors.INVALID_SUPPORT_REASON));
    }

    @Test
    @DisplayName("null or blank ticketId raises INVALID_SUPPORT_TICKET")
    void blankTicketId_raisesError() {
        SupportRequestDTO requestNullTicket = new SupportRequestDTO("Investigate memory leak", null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(requestNullTicket));
        assertTrue(context.getValidationErrors().get("ticketId").contains(ValidationErrors.INVALID_SUPPORT_TICKET));

        TestContextHelper.tearDown();
        context = TestContextHelper.setUp();

        SupportRequestDTO requestBlankTicket = new SupportRequestDTO("Investigate memory leak", "   ");
        assertThrows(ValidationException.class, () -> validation.validateRequest(requestBlankTicket));
        assertTrue(context.getValidationErrors().get("ticketId").contains(ValidationErrors.INVALID_SUPPORT_TICKET));
    }

    @Test
    @DisplayName("both blank reason and blank ticketId record both validation errors")
    void bothBlank_recordsBothErrors() {
        SupportRequestDTO request = new SupportRequestDTO("", "  ");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("reason").contains(ValidationErrors.INVALID_SUPPORT_REASON));
        assertTrue(context.getValidationErrors().get("ticketId").contains(ValidationErrors.INVALID_SUPPORT_TICKET));
    }
}
