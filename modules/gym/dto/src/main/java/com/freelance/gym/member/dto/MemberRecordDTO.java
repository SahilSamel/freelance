package com.freelance.gym.member.dto;

import com.freelance.gym.credit.dto.MemberCreditDTO;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.referral.dto.MemberReferralDTO;
import com.freelance.identityManagement.PhoneNumber;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MemberRecordDTO(
        UUID userId,
        String username,
        String firstName,
        String lastName,
        PhoneNumber phoneNumber,
        LocalDateTime createdDate,
        List<UserPlanResponseDTO> plans,
        List<PaymentResponseDTO> payments,
        MemberCreditDTO credit,
        MemberReferralDTO referral
) implements Serializable {
}

