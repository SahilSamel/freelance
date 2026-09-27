package com.freelance.gym.plan.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record PlanCreateRequestDTO(
        String name,
        String description,
        BigDecimal defaultPrice,
        Integer defaultDuration
) implements Serializable {
}
