package com.freelance.gym.plan.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record PlanUpdateRequestDTO(
        UUID planId,
        String name,
        String description,
        BigDecimal defaultPrice,
        Integer defaultDuration,
        Boolean isActive
) implements Serializable {
}
