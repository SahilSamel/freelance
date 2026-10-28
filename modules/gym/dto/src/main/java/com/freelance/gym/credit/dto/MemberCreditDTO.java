package com.freelance.gym.credit.dto;

import java.math.BigDecimal;

public record MemberCreditDTO(
        BigDecimal balance,
        String notes
) {
}
