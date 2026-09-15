package com.freelance.alerts;

import com.freelance.event.IConsumer;

public enum AlertConsumer implements IConsumer {
    OUTBOUND_DISPATCH("alert.outbound.dispatch"),
    OUTBOUND_WHATSAPP("alert.outbound.whatsapp"),
    INBOUND_WHATSAPP("alert.inbound.whatsapp");

    private final String destination;

    AlertConsumer(String destination) {
        this.destination = destination;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    public static AlertConsumer fromChannel(AlertChannel channel) {
        if (channel == null) {
            return OUTBOUND_DISPATCH;
        }
        return switch (channel) {
            case WHATSAPP -> OUTBOUND_WHATSAPP;
        };
    }
}
