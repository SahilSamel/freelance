package com.freelance.gym.member.dto;

import com.freelance.gym.payment.dto.PaymentResponseDTO;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MemberUpdateResponseDTO(
        UUID userId,
        List<UserPlanResponseDTO> plans,
        List<PaymentResponseDTO> payments
) implements Serializable {
}
