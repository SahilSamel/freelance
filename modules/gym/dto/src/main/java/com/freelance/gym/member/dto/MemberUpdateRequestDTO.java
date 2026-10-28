package com.freelance.gym.member.dto;

import com.freelance.gym.credit.dto.MemberCreditDTO;
import com.freelance.gym.payment.dto.PaidPaymentPlanLinkDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.referral.dto.MemberReferralDTO;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record MemberUpdateRequestDTO(
        UUID userId,
        MemberProfileDTO profile,
        List<MemberPlanAssignmentDTO> plans,
        List<PaymentScheduleDTO> payments,
        List<PaidPaymentPlanLinkDTO> paidPaymentPlanLinks,
        MemberCreditDTO credit,
        MemberReferralDTO referral
) implements Serializable {
    public MemberUpdateRequestDTO(
            UUID userId,
            MemberProfileDTO profile,
            List<MemberPlanAssignmentDTO> plans,
            List<PaymentScheduleDTO> payments
    ) {
        this(userId, profile, plans, payments, null, null, null);
    }

    public MemberUpdateRequestDTO(
            UUID userId,
            MemberProfileDTO profile,
            List<MemberPlanAssignmentDTO> plans,
            List<PaymentScheduleDTO> payments,
            List<PaidPaymentPlanLinkDTO> paidPaymentPlanLinks
    ) {
        this(userId, profile, plans, payments, paidPaymentPlanLinks, null, null);
    }
}
