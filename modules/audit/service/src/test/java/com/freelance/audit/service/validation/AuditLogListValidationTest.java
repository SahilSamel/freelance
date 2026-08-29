package com.freelance.audit.service.validation;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.audit.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLogListValidation Tests")
class AuditLogListValidationTest {

    private TransactionContext context;
    private AuditLogListValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new AuditLogListValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid request passes validation")
    void validRequest_passes() {
        AuditLogListRequestDTO request = new AuditLogListRequestDTO(
                null, null, "POST", "/users", 200,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now(), 50, 0
        );
        assertDoesNotThrow(() -> validation.validateRequest(request));
    }

    @Test
    @DisplayName("non-positive limit raises INVALID_LIMIT")
    void nonPositiveLimit_raisesError() {
        AuditLogListRequestDTO request = new AuditLogListRequestDTO(
                null, null, null, null, null, null, null, 0, 0
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("limit").contains(ValidationErrors.INVALID_LIMIT));
    }

    @Test
    @DisplayName("negative offset raises INVALID_OFFSET")
    void negativeOffset_raisesError() {
        AuditLogListRequestDTO request = new AuditLogListRequestDTO(
                null, null, null, null, null, null, null, 10, -5
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("offset").contains(ValidationErrors.INVALID_OFFSET));
    }

    @Test
    @DisplayName("fromTimestamp after toTimestamp raises INVALID_DATE_RANGE")
    void invertedDates_raisesError() {
        Instant now = Instant.now();
        AuditLogListRequestDTO request = new AuditLogListRequestDTO(
                null, null, null, null, null, now, now.minus(1, ChronoUnit.HOURS), 10, 0
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("fromTimestamp").contains(ValidationErrors.INVALID_DATE_RANGE));
    }
}
