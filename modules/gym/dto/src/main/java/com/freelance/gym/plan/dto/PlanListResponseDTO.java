package com.freelance.gym.plan.dto;

import java.io.Serializable;
import java.util.List;

public record PlanListResponseDTO(
        List<PlanResponseDTO> plans
) implements Serializable {
}
