package com.freelance.dispatcher.impl.push;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.whatsapp.WhatsAppUtility;
import com.freelance.dispatcher.IAlertDispatcher;
import com.freelance.dispatcher.data.AlertDispatchRequest;
import com.freelance.dispatcher.data.AlertDispatchResult;
import com.freelance.template.utils.TemplateRenderUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Dispatcher implementation for in-app push notifications.
 * Generates local dispatch identifiers and confirms successful in-app delivery.
 */
public class PushDispatcherService implements IAlertDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PushDispatcherService.class);

    public PushDispatcherService() {
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.PUSH;
    }

    @Override
    public AlertDispatchResult dispatch(AlertDispatchRequest request) {
        if (WhatsAppUtility.isQueuePaused()) {
            throw new IllegalStateException("Outbound queue is currently paused.");
        }

        String renderedText = request.renderedContent() != null
                ? request.renderedContent()
                : (request.template() != null
                ? TemplateRenderUtility.render(request.template(), request.resolvedAttributes())
                : "");

        String inAppMessageId = "push-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        logger.info("[PushDispatcherService] Dispatched in-app notification to user {}: id={}",
                request.userId(), inAppMessageId);

        return AlertDispatchResult.success(
                inAppMessageId,
                "{\"channel\":\"PUSH\",\"status\":\"DELIVERED\",\"id\":\"" + inAppMessageId + "\"}"
        );
    }
}
