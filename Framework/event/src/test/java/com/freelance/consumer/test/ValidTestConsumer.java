package com.freelance.consumer.test;

import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventConsumer;
import com.freelance.event.model.EventMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@EventConsumer(
        destination = "SCAN_TEST_DESTINATION",
        payloadType = ValidTestConsumer.ScanTestDTO.class,
        concurrency = 1
)
public class ValidTestConsumer implements EventHandler<ValidTestConsumer.ScanTestDTO> {

    public record ScanTestDTO(String text) {}

    public static final CountDownLatch testLatch = new CountDownLatch(1);
    public static final AtomicReference<ScanTestDTO> receivedDto = new AtomicReference<>();

    @Override
    public void handle(EventMessage<ScanTestDTO> message) {
        receivedDto.set(message.getPayload());
        testLatch.countDown();
    }
}
