package com.freelance.gym.member.dto;

import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.identityManagement.PhoneNumber;

import java.io.Serializable;
import java.util.List;

public record MemberCreateRequestDTO(
        String firstName,
        String lastName,
        PhoneNumber phoneNumber,
        List<MemberPlanAssignmentDTO> plans,
        List<PaymentScheduleDTO> payments
) implements Serializable {
}
