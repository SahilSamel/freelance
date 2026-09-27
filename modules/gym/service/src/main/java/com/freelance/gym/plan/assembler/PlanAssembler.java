package com.freelance.gym.plan.assembler;

import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanResponseDTO;
import com.freelance.gym.plan.entity.PlanEntity;

import java.math.BigDecimal;
import java.util.UUID;

public class PlanAssembler {

    public PlanEntity toEntity(PlanCreateRequestDTO request) {
        if (request == null) {
            return null;
        }
        return new PlanEntity(
                UUID.randomUUID(),
                request.name().trim(),
                request.description(),
                request.defaultPrice() != null ? request.defaultPrice() : BigDecimal.ZERO,
                request.defaultDuration() != null ? request.defaultDuration() : 30,
                true
        );
    }

    public PlanResponseDTO fromEntity(PlanEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PlanResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDefaultPrice(),
                entity.getDefaultDuration(),
                entity.isActive()
        );
    }
}
