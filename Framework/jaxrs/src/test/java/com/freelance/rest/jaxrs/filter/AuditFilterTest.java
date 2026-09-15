package com.freelance.rest.jaxrs.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditFilter Sanitization Unit Tests")
class AuditFilterTest {

    private final AuditFilter auditFilter = new AuditFilter();

    @Test
    @DisplayName("sanitizes password in JSON payload")
    void sanitizeRequestData_masksPasswordInJson() {
        String json = "{\"username\": \"john\", \"password\": \"Secret123!\"}";
        String sanitized = auditFilter.sanitizeRequestData(json);

        assertNotNull(sanitized);
        assertTrue(sanitized.contains("\"password\": \"******\""));
        assertFalse(sanitized.contains("Secret123!"));
        assertTrue(sanitized.contains("\"username\": \"john\""));
    }

    @Test
    @DisplayName("sanitizes token and secret in JSON payload")
    void sanitizeRequestData_masksTokenAndSecret() {
        String json = "{\"token\": \"eyJhbGciOi...\", \"secret\": \"my-app-secret\", \"name\": \"test\"}";
        String sanitized = auditFilter.sanitizeRequestData(json);

        assertNotNull(sanitized);
        assertTrue(sanitized.contains("\"token\": \"******\""));
        assertTrue(sanitized.contains("\"secret\": \"******\""));
        assertFalse(sanitized.contains("eyJhbGciOi..."));
        assertFalse(sanitized.contains("my-app-secret"));
        assertTrue(sanitized.contains("\"name\": \"test\""));
    }

    @Test
    @DisplayName("sanitizes query parameters with credentials")
    void sanitizeRequestData_masksQueryParameters() {
        String query = "query: page=1&password=myPass&token=abc12345&limit=20";
        String sanitized = auditFilter.sanitizeRequestData(query);

        assertNotNull(sanitized);
        assertTrue(sanitized.contains("password=******"));
        assertTrue(sanitized.contains("token=******"));
        assertFalse(sanitized.contains("myPass"));
        assertFalse(sanitized.contains("abc12345"));
        assertTrue(sanitized.contains("page=1"));
        assertTrue(sanitized.contains("limit=20"));
    }

    @Test
    @DisplayName("returns null or blank gracefully when data is empty")
    void sanitizeRequestData_handlesNullAndBlank() {
        assertNull(auditFilter.sanitizeRequestData(null));
        assertEquals("", auditFilter.sanitizeRequestData(""));
        assertEquals("   ", auditFilter.sanitizeRequestData("   "));
    }
}
