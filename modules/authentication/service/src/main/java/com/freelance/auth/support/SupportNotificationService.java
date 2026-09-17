package com.freelance.auth.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for dispatching access request notifications (e.g. push notifications,
 * WhatsApp alerts, email) to the client business administrator when tech support requests access.
 */
public class SupportNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SupportNotificationService.class);

    public void notifyAdminOfSupportRequest(String reason, String ticketId) {
        String safeReason = reason != null ? reason : "Technical diagnostic assistance";
        String safeTicket = ticketId != null ? ticketId : "N/A";

        // Formatted operational banner
        logger.info("================================================================================");
        logger.info("[SUPPORT ACCESS REQUEST NOTIFICATION DISPATCHED]");
        logger.info("  Ticket ID : {}", safeTicket);
        logger.info("  Reason    : {}", safeReason);
        logger.info("  Action Required: Business Administrator must grant access via POST /support/grant-access");
        logger.info("================================================================================");

        // Extension Point: integrate with WhatsApp / Push notifications
        dispatchPushNotification(safeReason, safeTicket);
    }

    private void dispatchPushNotification(String reason, String ticketId) {
        // Placeholder for real push / WhatsApp dispatcher hook
        logger.debug("[SupportNotificationService] Simulated push notification dispatched to business admin.");
    }
}

