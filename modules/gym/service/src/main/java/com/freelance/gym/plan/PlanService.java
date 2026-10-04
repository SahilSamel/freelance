package com.freelance.gym.plan;

import com.freelance.gym.member.dto.MemberPlanAssignmentDTO;
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.plan.assembler.PlanAssembler;
import com.freelance.gym.plan.constraint.PlanCreateConstraint;
import com.freelance.gym.plan.constraint.PlanDeleteConstraint;
import com.freelance.gym.plan.constraint.PlanUpdateConstraint;
import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.gym.plan.dto.PlanListResponseDTO;
import com.freelance.gym.plan.dto.PlanResponseDTO;
import com.freelance.gym.plan.dto.PlanUpdateRequestDTO;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.plan.validation.PlanCreateValidation;
import com.freelance.gym.plan.validation.PlanDeleteValidation;
import com.freelance.gym.plan.validation.PlanUpdateValidation;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlanService {

    private final PlanRepository planRepository;
    private final UserPlanRepository userPlanRepository;
    private final PlanAssembler assembler;

    public PlanService() {
        this(new PlanRepository(), new UserPlanRepository(), new PlanAssembler());
    }

    public PlanService(
            PlanRepository planRepository,
            UserPlanRepository userPlanRepository,
            PlanAssembler assembler
    ) {
        this.planRepository = planRepository;
        this.userPlanRepository = userPlanRepository;
        this.assembler = assembler;
    }

    public PlanResponseDTO createPlan(PlanCreateRequestDTO request) {
        new PlanCreateValidation().validateRequest(request);
        new PlanCreateConstraint(planRepository).checkConstraints(request);

        PlanEntity entity = assembler.toEntity(request);
        planRepository.persist(entity);

        return assembler.fromEntity(entity);
    }

    public PlanResponseDTO updatePlan(PlanUpdateRequestDTO request) {
        new PlanUpdateValidation().validateRequest(request);
        new PlanUpdateConstraint(planRepository).checkConstraints(request);

        PlanEntity entity = planRepository.get(request.planId());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        if (request.defaultPrice() != null) {
            entity.setDefaultPrice(request.defaultPrice());
        }
        if (request.defaultDuration() != null) {
            entity.setDefaultDuration(request.defaultDuration());
        }
        if (request.isActive() != null) {
            entity.setActive(request.isActive());
        }

        planRepository.merge(entity);
        return assembler.fromEntity(entity);
    }

    public PlanResponseDTO deletePlan(PlanDeleteRequestDTO request) {
        new PlanDeleteValidation().validateRequest(request);
        new PlanDeleteConstraint(planRepository).checkConstraints(request);

        PlanEntity entity = planRepository.get(request.planId());
        entity.setActive(false);
        planRepository.merge(entity);

        return assembler.fromEntity(entity);
    }

    public PlanListResponseDTO listPlans() {
        List<PlanEntity> allPlans = planRepository.findAllPlans();
        List<PlanResponseDTO> responseList = allPlans.stream()
                .map(assembler::fromEntity)
                .toList();

        return new PlanListResponseDTO(responseList);
    }

    public List<UserPlanResponseDTO> assignPlansToUser(UUID userId, List<MemberPlanAssignmentDTO> planAssignments) {
        if (planAssignments == null || planAssignments.isEmpty()) {
            return List.of();
        }

        List<UUID> planIds = planAssignments.stream().map(MemberPlanAssignmentDTO::planId).toList();
        List<PlanEntity> plans = planRepository.findByIds(planIds);
        Map<UUID, PlanEntity> planMap = plans.stream().collect(Collectors.toMap(PlanEntity::getId, p -> p));

        List<UserPlanResponseDTO> responseList = new ArrayList<>();

        for (MemberPlanAssignmentDTO dto : planAssignments) {
            PlanEntity plan = planMap.get(dto.planId());
            String planName = plan != null ? plan.getName() : "Unknown Plan";
            int durationDays = (plan != null && plan.getDefaultDuration() != null && plan.getDefaultDuration() > 0)
                    ? plan.getDefaultDuration()
                    : 30;
            LocalDate start = dto.startDate() != null ? dto.startDate() : LocalDate.now();
            LocalDate end = dto.endDate() != null ? dto.endDate() : start.plusDays(durationDays);

            UserPlanEntity userPlan = new UserPlanEntity(
                    UUID.randomUUID(),
                    userId,
                    dto.planId(),
                    start,
                    end
            );
            userPlanRepository.persist(userPlan);

            responseList.add(new UserPlanResponseDTO(
                    userPlan.getId(),
                    userId,
                    dto.planId(),
                    planName,
                    start,
                    end
            ));
        }

        return responseList;
    }
}
