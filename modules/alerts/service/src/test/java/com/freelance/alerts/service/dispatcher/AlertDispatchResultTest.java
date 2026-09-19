package com.freelance.alerts.service.dispatcher;

import com.freelance.dispatcher.data.AlertDispatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AlertDispatchResult Tests")
class AlertDispatchResultTest {

    @Test
    @DisplayName("success factory creates SUCCESS status result")
    void testSuccess() {
        AlertDispatchResult res = AlertDispatchResult.success("msg-123", "{\"status\":\"ok\"}");
        assertTrue(res.isSuccess());
        assertFalse(res.isFailure());
        assertFalse(res.isDelayed());
        assertEquals("msg-123", res.externalMessageId());
        assertEquals("{\"status\":\"ok\"}", res.rawResponse());
        assertNull(res.errorCode());
        assertNull(res.errorMessage());
    }

    @Test
    @DisplayName("failure factory creates FAILURE status result")
    void testFailure() {
        AlertDispatchResult res = AlertDispatchResult.failure("ERR_01", "Rate limited", "{\"error\":\"rate_limit\"}");
        assertFalse(res.isSuccess());
        assertTrue(res.isFailure());
        assertFalse(res.isDelayed());
        assertEquals("ERR_01", res.errorCode());
        assertEquals("Rate limited", res.errorMessage());
        assertEquals("{\"error\":\"rate_limit\"}", res.rawResponse());
    }

    @Test
    @DisplayName("delayed factory creates DELAYED status result")
    void testDelayed() {
        AlertDispatchResult res = AlertDispatchResult.delayed("Retry later");
        assertFalse(res.isSuccess());
        assertFalse(res.isFailure());
        assertTrue(res.isDelayed());
        assertEquals("Retry later", res.errorMessage());
    }
}
