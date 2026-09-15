package com.freelance.consumer.alerts.outbound.whatsapp;

import com.freelance.alerts.LogicalAlertAction;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.whatsapp.WhatsAppUtility;

/**
 * Backward-compatible adapter forwarding to {@link WhatsAppUtility}.
 */
public class WhatsAppErrorAdapter {

    public record ErrorResolution(
            String normalizedCode,
            String description,
            LogicalAlertAction action,
            boolean isRetriable
    ) {}

    public static boolean isQueuePaused() {
        return WhatsAppUtility.isQueuePaused();
    }

    public static void pauseQueue() {
        WhatsAppUtility.pauseQueue();
    }

    public static void resumeQueue() {
        WhatsAppUtility.resumeQueue();
    }

    public static ErrorResolution resolve(int metaErrorCode, Integer metaErrorSubcode, String metaMessage) {
        WhatsAppUtility.ErrorResolution res = WhatsAppUtility.resolveMetaError(metaErrorCode, metaErrorSubcode, metaMessage);
        return new ErrorResolution(res.normalizedCode(), res.description(), res.action(), res.isRetriable());
    }
}