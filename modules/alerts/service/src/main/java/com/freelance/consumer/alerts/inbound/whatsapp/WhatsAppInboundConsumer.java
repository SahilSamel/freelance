package com.freelance.consumer.alerts.inbound.whatsapp;

import com.freelance.alerts.AlertsEngine;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.webhook.WhatsAppWebhookPayloadDTO;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.repository.AlertButtonReplyRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.whatsapp.WhatsAppAlertProcessor;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventConsumer;
import com.freelance.event.model.EventMessage;

/**
 * Lean inbound WhatsApp webhook event consumer.
 * Purely responsible for event consumption and queue listening.
 * Delegates all processing to {@link WhatsAppAlertProcessor}.
 */
@EventConsumer(
        destination = "alert.inbound.whatsapp",
        payloadType = WhatsAppWebhookPayloadDTO.class,
        concurrency = 8,
        maxRetries = 2
)
public class WhatsAppInboundConsumer implements EventHandler<WhatsAppWebhookPayloadDTO> {

    private final WhatsAppAlertProcessor processor;

    public WhatsAppInboundConsumer() {
        this(new WhatsAppAlertProcessor());
    }

    public WhatsAppInboundConsumer(WhatsAppAlertProcessor processor) {
        this.processor = processor;
    }

    public WhatsAppInboundConsumer(
            AlertMessageRepository messageRepository,
            UserContactRepository contactRepository,
            AlertChannelConfigRepository channelConfigRepository,
            AlertButtonReplyRepository buttonReplyRepository,
            AlertTemplateRepository templateRepository
    ) {
        this(new WhatsAppAlertProcessor(
                messageRepository,
                contactRepository,
                channelConfigRepository,
                buttonReplyRepository,
                templateRepository,
                AlertsEngine.getInstance()
        ));
    }

    public WhatsAppInboundConsumer(
            AlertMessageRepository messageRepository,
            UserContactRepository contactRepository,
            AlertChannelConfigRepository channelConfigRepository,
            AlertButtonReplyRepository buttonReplyRepository,
            AlertTemplateRepository templateRepository,
            AlertsEngine alertsEngine
    ) {
        this(new WhatsAppAlertProcessor(
                messageRepository,
                contactRepository,
                channelConfigRepository,
                buttonReplyRepository,
                templateRepository,
                alertsEngine
        ));
    }

    @Override
    public void handle(EventMessage<WhatsAppWebhookPayloadDTO> eventMessage) {
        processor.processInbound(eventMessage);
    }
}
