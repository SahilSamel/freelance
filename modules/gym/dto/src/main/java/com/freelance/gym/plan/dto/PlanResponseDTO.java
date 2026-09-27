package com.freelance.gym.plan.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record PlanResponseDTO(
        UUID id,
        String name,
        String description,
        BigDecimal defaultPrice,
        Integer defaultDuration,
        boolean isActive
) implements Serializable {
}
