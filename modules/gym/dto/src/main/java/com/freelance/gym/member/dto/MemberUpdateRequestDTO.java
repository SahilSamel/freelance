package com.freelance.gym.member.dto;

import com.freelance.gym.payment.dto.PaymentScheduleDTO;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MemberUpdateRequestDTO(
        UUID userId,
        MemberProfileDTO profile,
        List<MemberPlanAssignmentDTO> plans,
        List<PaymentScheduleDTO> payments
) implements Serializable {
}
