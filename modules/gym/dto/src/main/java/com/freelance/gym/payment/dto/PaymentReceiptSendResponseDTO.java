package com.freelance.gym.payment.dto;

import java.io.Serializable;
import java.util.UUID;

public record PaymentReceiptSendResponseDTO(
        UUID paymentId,
        boolean success,
        String message
) implements Serializable {
}
