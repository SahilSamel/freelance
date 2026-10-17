package com.freelance.gym.payment.dto;

import com.freelance.gym.enums.PaymentMethod;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentCollectRequestDTO(
        UUID paymentId,
        BigDecimal paidAmount,
        LocalDate paidDate,
        PaymentMethod paymentMethod,
        String transactionId,
        String notes
) implements Serializable {
}
