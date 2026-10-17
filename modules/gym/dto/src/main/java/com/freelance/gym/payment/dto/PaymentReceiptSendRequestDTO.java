package com.freelance.gym.payment.dto;

import java.io.Serializable;
import java.util.UUID;

public record PaymentReceiptSendRequestDTO(
        UUID paymentId,
        UUID userId
) implements Serializable {
    public PaymentReceiptSendRequestDTO(UUID paymentId) {
        this(paymentId, null);
    }
}
