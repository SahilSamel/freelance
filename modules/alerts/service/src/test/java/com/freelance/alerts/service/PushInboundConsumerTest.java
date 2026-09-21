package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.NotificationReadRequestDTO;
import com.freelance.alerts.push.PushAlertProcessor;
import com.freelance.consumer.alerts.inbound.push.PushInboundConsumer;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PushInboundConsumer Unit Tests")
class PushInboundConsumerTest {

    @Test
    @DisplayName("PushInboundConsumer reports correct channel PUSH")
    void testChannel() {
        PushInboundConsumer consumer = new PushInboundConsumer();
        assertEquals(AlertChannel.PUSH, consumer.getChannel());
    }

    @Test
    @DisplayName("PushInboundConsumer delegates message to processor")
    void testHandleDelegatesToProcessor() {
        AtomicBoolean processed = new AtomicBoolean(false);
        PushAlertProcessor fakeProcessor = new PushAlertProcessor() {
            @Override
            public void processInbound(EventMessage<NotificationReadRequestDTO> message) {
                processed.set(true);
            }
        };

        PushInboundConsumer consumer = new PushInboundConsumer(fakeProcessor);
        EventMessage<NotificationReadRequestDTO> message = EventMessage.of(
                "alert.inbound.push",
                new NotificationReadRequestDTO(UUID.randomUUID()),
                EventPriority.HIGH
        );

        consumer.handle(message);
        assertTrue(processed.get());
    }
}
