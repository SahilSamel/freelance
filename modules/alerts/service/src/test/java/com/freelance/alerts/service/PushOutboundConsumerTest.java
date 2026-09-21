package com.freelance.alerts.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.AlertEventPayloadDTO;
import com.freelance.alerts.push.PushAlertProcessor;
import com.freelance.consumer.alerts.outbound.push.PushOutboundConsumer;
import com.freelance.event.model.EventMessage;
import com.freelance.event.model.EventPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PushOutboundConsumer Unit Tests")
class PushOutboundConsumerTest {

    @Test
    @DisplayName("PushOutboundConsumer reports correct channel PUSH")
    void testChannel() {
        PushOutboundConsumer consumer = new PushOutboundConsumer();
        assertEquals(AlertChannel.PUSH, consumer.getChannel());
    }

    @Test
    @DisplayName("PushOutboundConsumer delegates message to processor")
    void testHandleDelegatesToProcessor() {
        AtomicBoolean processed = new AtomicBoolean(false);
        PushAlertProcessor fakeProcessor = new PushAlertProcessor() {
            @Override
            public void processOutbound(EventMessage<AlertEventPayloadDTO> message) {
                processed.set(true);
            }
        };

        PushOutboundConsumer consumer = new PushOutboundConsumer(fakeProcessor);
        EventMessage<AlertEventPayloadDTO> message = EventMessage.of(
                "alert.outbound.push",
                new AlertEventPayloadDTO(null, UUID.randomUUID(), "Hello", UUID.randomUUID()),
                EventPriority.HIGH
        );

        consumer.handle(message);
        assertTrue(processed.get());
    }
}
