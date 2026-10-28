package com.freelance.gym.referral.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public record MemberReferralDTO(
        UUID referrerId,
        String referrerName,
        LocalDate referralDate,
        String notes
) implements Serializable {
}
