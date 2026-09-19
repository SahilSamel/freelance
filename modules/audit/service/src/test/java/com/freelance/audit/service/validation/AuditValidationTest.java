package com.freelance.audit.service.validation;

import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.audit.http.HttpAuditValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditValidation Unit Tests")
class AuditValidationTest {

    private TransactionContext context;
    private final AuditValidation validation = new AuditValidation();

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should fail when request is null")
    void testNullRequest() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertTrue(context.getValidationErrors().get("request").contains(HttpAuditValidationErrors.MISSING_HTTP_METHOD));
    }

    @Test
    @DisplayName("Should fail when httpMethod is null or blank")
    void testMissingHttpMethod() {
        AuditLogCreateRequestDTO req = new AuditLogCreateRequestDTO(
                Instant.now(), "trace-1", UUID.randomUUID(), "", "/api/test", null, 200, 50, "127.0.0.1", "Agent", "bearer"
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().get("httpMethod").contains(HttpAuditValidationErrors.MISSING_HTTP_METHOD));
    }

    @Test
    @DisplayName("Should fail when endpoint is null or blank")
    void testMissingEndpoint() {
        AuditLogCreateRequestDTO req = new AuditLogCreateRequestDTO(
                Instant.now(), "trace-1", UUID.randomUUID(), "GET", "  ", null, 200, 50, "127.0.0.1", "Agent", "bearer"
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().get("endpoint").contains(HttpAuditValidationErrors.MISSING_ENDPOINT));
    }

    @Test
    @DisplayName("Should fail when statusCode is zero or negative")
    void testMissingStatusCode() {
        AuditLogCreateRequestDTO req = new AuditLogCreateRequestDTO(
                Instant.now(), "trace-1", UUID.randomUUID(), "GET", "/api/test", null, 0, 50, "127.0.0.1", "Agent", "bearer"
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().get("statusCode").contains(HttpAuditValidationErrors.MISSING_STATUS_CODE));
    }

    @Test
    @DisplayName("Should fail when timestamp is null")
    void testMissingTimestamp() {
        AuditLogCreateRequestDTO req = new AuditLogCreateRequestDTO(
                null, "trace-1", UUID.randomUUID(), "GET", "/api/test", null, 200, 50, "127.0.0.1", "Agent", "bearer"
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().get("timestamp").contains(HttpAuditValidationErrors.MISSING_TIMESTAMP));
    }

    @Test
    @DisplayName("Should fail when durationMs is negative")
    void testInvalidDuration() {
        AuditLogCreateRequestDTO req = new AuditLogCreateRequestDTO(
                Instant.now(), "trace-1", UUID.randomUUID(), "GET", "/api/test", null, 200, -5, "127.0.0.1", "Agent", "bearer"
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().get("durationMs").contains(HttpAuditValidationErrors.INVALID_DURATION));
    }

    @Test
    @DisplayName("Should fail when traceId is null or blank")
    void testMissingTraceId() {
        AuditLogCreateRequestDTO req = new AuditLogCreateRequestDTO(
                Instant.now(), "  ", UUID.randomUUID(), "GET", "/api/test", null, 200, 50, "127.0.0.1", "Agent", "bearer"
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(req));
        assertTrue(context.getValidationErrors().get("traceId").contains(HttpAuditValidationErrors.MISSING_TRACE_ID));
    }

    @Test
    @DisplayName("Should pass when all fields are valid")
    void testValidRequestPasses() {
        AuditLogCreateRequestDTO req = new AuditLogCreateRequestDTO(
                Instant.now(), "trace-123", UUID.randomUUID(), "POST", "/api/users", "{\"name\":\"test\"}", 201, 12, "127.0.0.1", "Agent", "bearer"
        );
        assertDoesNotThrow(() -> validation.validateRequest(req));
    }
}
