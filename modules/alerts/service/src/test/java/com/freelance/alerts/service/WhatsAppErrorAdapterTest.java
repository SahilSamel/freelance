package com.freelance.alerts.service;

import com.freelance.alerts.LogicalAlertAction;
import com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WhatsAppErrorAdapterTest {

    @Test
    @DisplayName("Should map recipient not on WhatsApp to DISABLE_USER_CHANNEL")
    void testRecipientNotOnWhatsApp() {
        WhatsAppErrorAdapter.ErrorResolution res = WhatsAppErrorAdapter.resolve(131026, null, "Recipient not on WhatsApp");
        assertEquals("ERR_USER_NOT_ON_CHANNEL", res.normalizedCode());
        assertEquals(LogicalAlertAction.DISABLE_USER_CHANNEL, res.action());
        assertFalse(res.isRetriable());
    }

    @Test
    @DisplayName("Should map user block/opt-out to MARK_USER_OPTED_OUT")
    void testUserBlockedBusiness() {
        WhatsAppErrorAdapter.ErrorResolution res = WhatsAppErrorAdapter.resolve(131049, null, "User blocked business");
        assertEquals("ERR_USER_BLOCKED_BUSINESS", res.normalizedCode());
        assertEquals(LogicalAlertAction.MARK_USER_OPTED_OUT, res.action());
        assertFalse(res.isRetriable());
    }

    @Test
    @DisplayName("Should map 24h window closed to TRIGGER_HUMAN_INTERVENTION")
    void testCustomerServiceWindowClosed() {
        WhatsAppErrorAdapter.ErrorResolution res = WhatsAppErrorAdapter.resolve(131047, null, "Message failed outside 24h window");
        assertEquals("ERR_24H_WINDOW_EXPIRED", res.normalizedCode());
        assertEquals(LogicalAlertAction.TRIGGER_HUMAN_INTERVENTION, res.action());
        assertFalse(res.isRetriable());
    }

    @Test
    @DisplayName("Should map account locked / revoked tokens to CRITICAL_ADMIN_ALERT")
    void testCriticalAccountAndAuthErrors() {
        WhatsAppErrorAdapter.ErrorResolution locked = WhatsAppErrorAdapter.resolve(131031, null, "Account locked");
        assertEquals("ERR_BUSINESS_ACCOUNT_LOCKED", locked.normalizedCode());
        assertEquals(LogicalAlertAction.CRITICAL_ADMIN_ALERT, locked.action());

        WhatsAppErrorAdapter.ErrorResolution auth = WhatsAppErrorAdapter.resolve(190, null, "OAuth token expired");
        assertEquals("ERR_META_AUTH_EXPIRED", auth.normalizedCode());
        assertEquals(LogicalAlertAction.CRITICAL_ADMIN_ALERT, auth.action());

        WhatsAppErrorAdapter.ErrorResolution payment = WhatsAppErrorAdapter.resolve(131042, null, "Payment failure");
        assertEquals("ERR_META_PAYMENT_FAILED", payment.normalizedCode());
        assertEquals(LogicalAlertAction.CRITICAL_ADMIN_ALERT, payment.action());
    }

    @Test
    @DisplayName("Should map template paused to PAUSE_TEMPLATE")
    void testTemplatePaused() {
        WhatsAppErrorAdapter.ErrorResolution res = WhatsAppErrorAdapter.resolve(132001, null, "Template paused due to low quality");
        assertEquals("ERR_TEMPLATE_PAUSED", res.normalizedCode());
        assertEquals(LogicalAlertAction.PAUSE_TEMPLATE, res.action());
        assertFalse(res.isRetriable());
    }

    @Test
    @DisplayName("Should map rate limits and transient errors to RETRY_WITH_BACKOFF")
    void testRateLimitAndTransient() {
        WhatsAppErrorAdapter.ErrorResolution rateLimit = WhatsAppErrorAdapter.resolve(130429, null, "Rate limit reached");
        assertEquals("ERR_META_RATE_LIMITED", rateLimit.normalizedCode());
        assertEquals(LogicalAlertAction.RETRY_WITH_BACKOFF, rateLimit.action());
        assertTrue(rateLimit.isRetriable());

        WhatsAppErrorAdapter.ErrorResolution server500 = WhatsAppErrorAdapter.resolve(500, null, "Internal Meta error");
        assertEquals("ERR_META_RATE_LIMITED", server500.normalizedCode());
        assertTrue(server500.isRetriable());
    }

    @Test
    @DisplayName("Should pause and resume outbound queue")
    void testQueuePausing() {
        WhatsAppErrorAdapter.resumeQueue();
        assertFalse(WhatsAppErrorAdapter.isQueuePaused());

        WhatsAppErrorAdapter.pauseQueue();
        assertTrue(WhatsAppErrorAdapter.isQueuePaused());

        WhatsAppErrorAdapter.resumeQueue();
        assertFalse(WhatsAppErrorAdapter.isQueuePaused());
    }

    @Test
    @DisplayName("Should resolve WhatsAppErrorCode directly and expose rich metadata")
    void testWhatsAppErrorCode_directResolution() {
        com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode code =
                com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode.fromMeta(131047, null);

        assertEquals(com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode.WINDOW_24H_EXPIRED, code);
        assertEquals("ERR_24H_WINDOW_EXPIRED", code.getCode());
        assertEquals(LogicalAlertAction.TRIGGER_HUMAN_INTERVENTION, code.getAction());
        assertFalse(code.isRetriable());
        assertTrue(code.getDescription().contains("24 hours"));
    }

    @Test
    @DisplayName("Should correctly resolve user opt-out subcode 221004")
    void testWhatsAppErrorCode_optOutSubcode() {
        com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode code =
                com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode.fromMeta(131026, 221004);

        assertEquals(com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode.USER_OPTED_OUT, code);
        assertEquals("ERR_USER_OPTED_OUT", code.getCode());
        assertEquals(LogicalAlertAction.MARK_USER_OPTED_OUT, code.getAction());
    }

    @Test
    @DisplayName("Should fall back to UNKNOWN for unmapped Meta error codes")
    void testWhatsAppErrorCode_unknownFallback() {
        com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode code =
                com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode.fromMeta(999999, null);

        assertEquals(com.freelance.consumer.alerts.outbound.whatsapp.WhatsAppErrorCode.UNKNOWN, code);
        assertEquals("ERR_META_UNKNOWN", code.getCode());

        WhatsAppErrorAdapter.ErrorResolution res = WhatsAppErrorAdapter.resolve(999999, null, "Custom gateway failure");
        assertEquals("ERR_META_UNKNOWN_999999", res.normalizedCode());
        assertEquals("Custom gateway failure", res.description());
    }
}