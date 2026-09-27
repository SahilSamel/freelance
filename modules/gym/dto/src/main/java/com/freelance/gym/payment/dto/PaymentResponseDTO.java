package com.freelance.gym.payment.dto;

import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.member.dto.UserPlanResponseDTO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PaymentResponseDTO(
        UUID id,
        BigDecimal amount,
        BigDecimal paidAmount,
        LocalDate dueDate,
        LocalDate paidDate,
        PaymentMethod paymentMethod,
        String transactionId,
        String notes,
        List<UserPlanResponseDTO> plans
) implements Serializable {
}

