package com.freelance.gym.plan.dto;

import java.io.Serializable;
import java.util.UUID;

public record PlanDeleteRequestDTO(
        UUID planId
) implements Serializable {
}
