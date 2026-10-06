package com.freelance.consumer.alerts.outbound.whatsapp;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.alerts.whatsapp.WhatsAppAlertProcessor;
import com.freelance.dispatcher.AlertDispatcherFactory;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventConsumer;
import com.freelance.event.model.EventMessage;
import com.freelance.user.read.UserReadService;

/**
 * Lean outbound WhatsApp alert consumer.
 * Purely responsible for event consumption and queue listening.
 * Delegates all processing to {@link WhatsAppAlertProcessor}.
 */
@EventConsumer(
        destination = "alert.outbound.whatsapp",
        payloadType = AlertEventPayloadDTO.class,
        concurrency = 35,
        rateLimitPerSecond = 50.0,
        maxRetries = 3,
        backoffMillis = 86_400_000L,
        backoffMultiplier = 1.0
)
public class WhatsAppOutboundConsumer implements EventHandler<AlertEventPayloadDTO> {

    private final WhatsAppAlertProcessor processor;

    public WhatsAppOutboundConsumer() {
        this(new WhatsAppAlertProcessor());
    }

    public WhatsAppOutboundConsumer(WhatsAppAlertProcessor processor) {
        this.processor = processor;
    }

    public WhatsAppOutboundConsumer(
            UserReadService userReadService,
            AlertTemplateRepository templateRepository,
            UserContactRepository contactRepository,
            AlertChannelConfigRepository channelConfigRepository,
            AlertMessageRepository messageRepository,
            AlertDispatcherFactory dispatcherFactory
    ) {
        this(new WhatsAppAlertProcessor(
                userReadService,
                templateRepository,
                contactRepository,
                channelConfigRepository,
                messageRepository,
                dispatcherFactory
        ));
    }

    public AlertChannel getChannel() {
        return AlertChannel.WHATSAPP;
    }

    @Override
    public void handle(EventMessage<AlertEventPayloadDTO> message) {
        processor.processOutbound(message);
    }

    @Override
    public void onDeadLetter(EventMessage<AlertEventPayloadDTO> message, Throwable cause) {
        processor.handleOutboundDeadLetter(message, cause);
    }
}
