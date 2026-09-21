package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.NotificationReadService;
import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationReadServiceTest {

    private TransactionContext context;
    private NotificationReadService service;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        service = new NotificationReadService();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should successfully process valid mark as read request")
    void testMarkAsRead_success() {
        NotificationReadRequestDTO request = new NotificationReadRequestDTO(
                UUID.randomUUID(),
                List.of(UUID.randomUUID()),
                AlertChannel.PUSH
        );

        assertDoesNotThrow(() -> service.markAsRead(request));
    }

    @Test
    @DisplayName("Should throw ValidationException when request is null")
    void testMarkAsRead_nullRequest() {
        assertThrows(ValidationException.class, () -> service.markAsRead(null));
    }

    @Test
    @DisplayName("Should throw ValidationException when userId is null")
    void testMarkAsRead_nullUserId() {
        NotificationReadRequestDTO request = new NotificationReadRequestDTO(null, null, AlertChannel.PUSH);
        assertThrows(ValidationException.class, () -> service.markAsRead(request));
    }
}
