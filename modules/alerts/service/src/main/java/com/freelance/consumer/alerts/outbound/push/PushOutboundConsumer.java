package com.freelance.consumer.alerts.outbound.push;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.channel.repository.AlertChannelConfigRepository;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.message.repository.AlertMessageRepository;
import com.freelance.alerts.push.PushAlertProcessor;
import com.freelance.alerts.template.repository.AlertTemplateRepository;
import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventConsumer;
import com.freelance.event.model.EventMessage;
import com.freelance.user.read.UserReadService;

/**
 * Outbound Push / In-App notification alert consumer.
 * Listens on destination {@code alert.outbound.push} and delegates processing
 * to {@link PushAlertProcessor}.
 */
@EventConsumer(
        destination = "alert.outbound.push",
        payloadType = AlertEventPayloadDTO.class,
        concurrency = 1,
        maxRetries = 3
)
public class PushOutboundConsumer implements EventHandler<AlertEventPayloadDTO> {

    private final PushAlertProcessor processor;

    public PushOutboundConsumer() {
        this(new PushAlertProcessor());
    }

    public PushOutboundConsumer(PushAlertProcessor processor) {
        this.processor = processor != null ? processor : new PushAlertProcessor();
    }

    public PushOutboundConsumer(
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
    public void handle(EventMessage<AlertEventPayloadDTO> message) {
        processor.processOutbound(message);
    }
}
