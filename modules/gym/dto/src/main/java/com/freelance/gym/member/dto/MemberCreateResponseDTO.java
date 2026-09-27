package com.freelance.gym.member.dto;

import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.identityManagement.PhoneNumber;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MemberCreateResponseDTO(
        UUID userId,
        String username,
        String generatedPassword,
        String firstName,
        String lastName,
        PhoneNumber phoneNumber,
        List<String> userTypes,
        List<UserPlanResponseDTO> plans,
        List<PaymentResponseDTO> payments
) implements Serializable {
}
