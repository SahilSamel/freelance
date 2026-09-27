package com.freelance.gym.member.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public record MemberPlanAssignmentDTO(
        UUID planId,
        LocalDate startDate,
        LocalDate endDate
) implements Serializable {
}
