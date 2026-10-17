package com.freelance.gym.payment.dto;

import com.freelance.gym.enums.PaymentMethod;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentScheduleDTO(
        BigDecimal amount,
        BigDecimal paidAmount,
        LocalDate dueDate,
        LocalDate paidDate,
        PaymentMethod paymentMethod,
        String transactionId,
        String notes,
        List<Integer> planIndices
) implements Serializable {
    public PaymentScheduleDTO(
            BigDecimal amount,
            BigDecimal paidAmount,
            LocalDate dueDate,
            LocalDate paidDate,
            PaymentMethod paymentMethod,
            String transactionId,
            String notes
    ) {
        this(amount, paidAmount, dueDate, paidDate, paymentMethod, transactionId, notes, null);
    }
}
