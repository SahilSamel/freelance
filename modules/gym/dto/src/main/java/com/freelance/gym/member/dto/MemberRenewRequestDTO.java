package com.freelance.gym.member.dto;

import com.freelance.gym.payment.dto.PaymentScheduleDTO;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MemberRenewRequestDTO(
        UUID userId,
        List<MemberPlanAssignmentDTO> plans,
        List<PaymentScheduleDTO> payments
) implements Serializable {
}
