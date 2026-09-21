package com.freelance.consumer.alerts.inbound.push;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.push.PushAlertProcessor;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventConsumer;
import com.freelance.event.model.EventMessage;
import com.freelance.user.read.UserReadService;

/**
 * Inbound Push / In-App notification read receipt consumer.
 * Listens on destination {@code alert.inbound.push} and delegates processing
 * to {@link PushAlertProcessor}.
 */
@EventConsumer(
        destination = "alert.inbound.push",
        payloadType = NotificationReadRequestDTO.class,
        concurrency = 4,
        rateLimitPerSecond = 100.0,
        maxRetries = 3
)
public class PushInboundConsumer implements EventHandler<NotificationReadRequestDTO> {

    private final PushAlertProcessor processor;

    public PushInboundConsumer() {
        this(new PushAlertProcessor());
    }

    public PushInboundConsumer(PushAlertProcessor processor) {
        this.processor = processor != null ? processor : new PushAlertProcessor();
    }

    public PushInboundConsumer(
            UserReadService userReadService,
            AlertTemplateRepository templateRepository,
            AlertMessageRepository messageRepository,
            AlertChannelConfigRepository channelConfigRepository
    ) {
        this(new PushAlertProcessor(
                userReadService,
                templateRepository,
                messageRepository,
                channelConfigRepository
        ));
    }

    public AlertChannel getChannel() {
        return AlertChannel.PUSH;
    }

    @Override
    public void handle(EventMessage<NotificationReadRequestDTO> message) {
        processor.processInbound(message);
    }
}
