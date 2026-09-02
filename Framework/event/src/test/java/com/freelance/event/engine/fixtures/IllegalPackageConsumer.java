package com.freelance.event.engine.fixtures;

import com.freelance.event.consumer.EventHandler;
import com.freelance.event.model.EventConsumer;
import com.freelance.event.model.EventMessage;

@EventConsumer(
        destination = "ILLEGAL_PACKAGE_DESTINATION",
        payloadType = String.class,
        concurrency = 1
)
public class IllegalPackageConsumer implements EventHandler<String> {
    @Override
    public void handle(EventMessage<String> message) {
    }
}
