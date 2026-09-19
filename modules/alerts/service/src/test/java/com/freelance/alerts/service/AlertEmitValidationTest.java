package com.freelance.alerts.service;

import com.freelance.alerts.dto.AlertEmitRequestDTO;
import com.freelance.alerts.validation.AlertEmitValidation;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.alerts.message.AlertEmitValidationErrors;
import com.freelance.keys.alerts.template.TemplateValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AlertEmitValidationTest {

    private TransactionContext context;
    private AlertEmitValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new AlertEmitValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should throw ValidationException when templateId is null")
    void testNullTemplateId() {
        AlertEmitRequestDTO request = new AlertEmitRequestDTO(null, List.of(UUID.randomUUID()));

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("templateId"));
        assertTrue(context.getValidationErrors().get("templateId").contains(TemplateValidationErrors.INVALID_TEMPLATE_ID));
    }

    @Test
    @DisplayName("Should throw ValidationException when userIds list is empty")
    void testEmptyUserIds() {
        AlertEmitRequestDTO request = new AlertEmitRequestDTO(UUID.randomUUID(), Collections.emptyList());

        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().containsKey("userIds"));
        assertTrue(context.getValidationErrors().get("userIds").contains(AlertEmitValidationErrors.INVALID_USER_IDS));
    }

    @Test
    @DisplayName("Should throw ValidationException when request is null")
    void testNullRequest() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().containsKey("templateId"));
        assertTrue(context.getValidationErrors().containsKey("userIds"));
    }

    @Test
    @DisplayName("Should throw ValidationException when userIds list is null or contains null elements")
    void testNullUserIdsAndNullElements() {
        AlertEmitRequestDTO nullUserIdsReq = new AlertEmitRequestDTO(UUID.randomUUID(), null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(nullUserIdsReq));
        assertTrue(context.getValidationErrors().containsKey("userIds"));

        context = TestContextHelper.setUp();
        List<UUID> listWithNull = new ArrayList<>();
        listWithNull.add(UUID.randomUUID());
        listWithNull.add(null);
        AlertEmitRequestDTO nullElementReq = new AlertEmitRequestDTO(UUID.randomUUID(), listWithNull);
        assertThrows(ValidationException.class, () -> validation.validateRequest(nullElementReq));
        assertTrue(context.getValidationErrors().containsKey("userIds[1]"));
    }

    @Test
    @DisplayName("Should pass validation for valid request")
    void testValidRequest() {
        AlertEmitRequestDTO request = new AlertEmitRequestDTO(UUID.randomUUID(), List.of(UUID.randomUUID()));

        assertDoesNotThrow(() -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().isEmpty());
    }
}