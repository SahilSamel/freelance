package com.freelance.gym.member.dto;

import com.freelance.gym.payment.dto.PaymentResponseDTO;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MemberRenewResponseDTO(
        UUID userId,
        List<UserPlanResponseDTO> renewedPlans,
        List<PaymentResponseDTO> newPayments
) implements Serializable {
}
