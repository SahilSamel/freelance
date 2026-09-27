package com.freelance.gym.payment.dto;

import com.freelance.gym.enums.PaymentMethod;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentScheduleDTO(
        BigDecimal amount,
        BigDecimal paidAmount,
        LocalDate dueDate,
        LocalDate paidDate,
        PaymentMethod paymentMethod,
        String transactionId,
        String notes
) implements Serializable {
}
