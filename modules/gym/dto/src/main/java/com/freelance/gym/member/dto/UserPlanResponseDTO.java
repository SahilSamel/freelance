package com.freelance.gym.member.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public record UserPlanResponseDTO(
        UUID id,
        UUID userId,
        UUID planId,
        String planName,
        LocalDate startDate,
        LocalDate endDate
) implements Serializable {
}
