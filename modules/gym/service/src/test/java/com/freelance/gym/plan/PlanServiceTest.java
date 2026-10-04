package com.freelance.gym.plan;

import com.freelance.gym.fixtures.TestContextHelper;
import com.freelance.gym.member.dto.MemberPlanAssignmentDTO;
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.plan.assembler.PlanAssembler;
import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.gym.plan.dto.PlanListResponseDTO;
import com.freelance.gym.plan.dto.PlanResponseDTO;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserPlanRepository userPlanRepository;

    private PlanService planService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        planService = new PlanService(planRepository, userPlanRepository, new PlanAssembler());
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new PlanService());
    }

    @Test
    void testCreatePlanSuccess() {
        when(planRepository.findByName("Personal Training")).thenReturn(Optional.empty());

        PlanCreateRequestDTO request = new PlanCreateRequestDTO(
                "Personal Training",
                "1-on-1 PT",
                BigDecimal.valueOf(2500),
                60
        );

        PlanResponseDTO response = planService.createPlan(request);
        assertNotNull(response);
        assertEquals("Personal Training", response.name());
        assertEquals(BigDecimal.valueOf(2500), response.defaultPrice());
        assertEquals(60, response.defaultDuration());
        assertTrue(response.isActive());

        verify(planRepository, times(1)).persist(any(PlanEntity.class));
    }

    @Test
    void testCreatePlanValidationFailureWhenNameEmpty() {
        PlanCreateRequestDTO request = new PlanCreateRequestDTO("", "Desc", BigDecimal.valueOf(1000), 30);
        assertThrows(ValidationException.class, () -> planService.createPlan(request));
    }

    @Test
    void testCreatePlanValidationFailureWhenDurationInvalid() {
        PlanCreateRequestDTO request = new PlanCreateRequestDTO("Valid Name", "Desc", BigDecimal.valueOf(1000), 0);
        assertThrows(ValidationException.class, () -> planService.createPlan(request));
    }

    @Test
    void testCreatePlanDuplicateNameFails() {
        when(planRepository.findByName("Gym Floor")).thenReturn(Optional.of(new PlanEntity(UUID.randomUUID(), "Gym Floor", "", BigDecimal.ZERO, 30, true)));
        PlanCreateRequestDTO request = new PlanCreateRequestDTO("Gym Floor", "Desc", BigDecimal.valueOf(1000), 30);
        assertThrows(ConstraintException.class, () -> planService.createPlan(request));
    }

    @Test
    void testDeletePlanSuccess() {
        UUID planId = UUID.randomUUID();
        PlanEntity plan = new PlanEntity(planId, "Zumba", "Dance", BigDecimal.valueOf(500), 30, true);
        when(planRepository.get(planId)).thenReturn(plan);

        PlanResponseDTO response = planService.deletePlan(new PlanDeleteRequestDTO(planId));
        assertNotNull(response);
        assertFalse(response.isActive());
        verify(planRepository, times(1)).merge(plan);
    }

    @Test
    void testListPlans() {
        UUID plan1 = UUID.randomUUID();
        UUID plan2 = UUID.randomUUID();
        PlanEntity p1 = new PlanEntity(plan1, "Gym Floor", "", BigDecimal.valueOf(1000), 30, true);
        PlanEntity p2 = new PlanEntity(plan2, "PT", "", BigDecimal.valueOf(2000), 45, true);

        when(planRepository.findAllPlans()).thenReturn(List.of(p1, p2));

        PlanListResponseDTO list = planService.listPlans();
        assertNotNull(list);
        assertEquals(2, list.plans().size());
        assertEquals("Gym Floor", list.plans().get(0).name());
        assertEquals(30, list.plans().get(0).defaultDuration());
        assertEquals("PT", list.plans().get(1).name());
        assertEquals(45, list.plans().get(1).defaultDuration());
    }

    @Test
    void testAssignPlansToUserCalculatesEndDateFromDefaultDuration() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        PlanEntity plan = new PlanEntity(planId, "Gym 45-Day Pass", "Access", BigDecimal.valueOf(1500), 45, true);
        when(planRepository.findByIds(List.of(planId))).thenReturn(List.of(plan));

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        List<MemberPlanAssignmentDTO> assignments = List.of(
                new MemberPlanAssignmentDTO(planId, startDate, null) // endDate is null
        );

        List<UserPlanResponseDTO> response = planService.assignPlansToUser(userId, assignments);
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(startDate, response.get(0).startDate());
        assertEquals(startDate.plusDays(45), response.get(0).endDate());

        verify(userPlanRepository, times(1)).persist(any(UserPlanEntity.class));
    }

    @Test
    void testAssignPlansToUser_EdgeCases() {
        UUID userId = UUID.randomUUID();

        // Null or empty assignments
        assertTrue(planService.assignPlansToUser(userId, null).isEmpty());
        assertTrue(planService.assignPlansToUser(userId, List.of()).isEmpty());

        // Plan not found in repo + null start date + explicit end date
        UUID missingPlanId = UUID.randomUUID();
        when(planRepository.findByIds(List.of(missingPlanId))).thenReturn(List.of());

        LocalDate explicitEnd = LocalDate.now().plusDays(20);
        List<MemberPlanAssignmentDTO> assignments1 = List.of(
                new MemberPlanAssignmentDTO(missingPlanId, null, explicitEnd)
        );

        List<UserPlanResponseDTO> res1 = planService.assignPlansToUser(userId, assignments1);
        assertEquals(1, res1.size());
        assertEquals("Unknown Plan", res1.get(0).planName());
        assertEquals(LocalDate.now(), res1.get(0).startDate());
        assertEquals(explicitEnd, res1.get(0).endDate());

        // Plan with null defaultDuration -> falls back to 30 days
        UUID planId2 = UUID.randomUUID();
        PlanEntity plan2 = new PlanEntity(planId2, "No Duration Plan", "Desc", BigDecimal.valueOf(500), null, true);
        when(planRepository.findByIds(List.of(planId2))).thenReturn(List.of(plan2));

        List<MemberPlanAssignmentDTO> assignments2 = List.of(
                new MemberPlanAssignmentDTO(planId2, null, null)
        );
        List<UserPlanResponseDTO> res2 = planService.assignPlansToUser(userId, assignments2);
        assertEquals(1, res2.size());
        assertEquals(LocalDate.now().plusDays(30), res2.get(0).endDate());

        // Plan with defaultDuration <= 0 -> falls back to 30 days
        UUID planId3 = UUID.randomUUID();
        PlanEntity plan3 = new PlanEntity(planId3, "Zero Duration Plan", "Desc", BigDecimal.valueOf(500), 0, true);
        when(planRepository.findByIds(List.of(planId3))).thenReturn(List.of(plan3));

        List<MemberPlanAssignmentDTO> assignments3 = List.of(
                new MemberPlanAssignmentDTO(planId3, null, null)
        );
        List<UserPlanResponseDTO> res3 = planService.assignPlansToUser(userId, assignments3);
        assertEquals(1, res3.size());
        assertEquals(LocalDate.now().plusDays(30), res3.get(0).endDate());
    }

    @Test
    void testUpdatePlanSuccess() {
        UUID planId = UUID.randomUUID();
        PlanEntity existing = new PlanEntity(planId, "Old Name", "Old Desc", BigDecimal.valueOf(1000), 30, true);
        when(planRepository.get(planId)).thenReturn(existing);
        when(planRepository.findByName("New Name")).thenReturn(Optional.empty());

        com.freelance.gym.plan.dto.PlanUpdateRequestDTO request = new com.freelance.gym.plan.dto.PlanUpdateRequestDTO(
                planId,
                "New Name",
                "New Desc",
                BigDecimal.valueOf(1500),
                45,
                true
        );

        PlanResponseDTO response = planService.updatePlan(request);
        assertNotNull(response);
        assertEquals("New Name", response.name());
        assertEquals("New Desc", response.description());
        assertEquals(BigDecimal.valueOf(1500), response.defaultPrice());
        assertEquals(45, response.defaultDuration());
        assertTrue(response.isActive());
        verify(planRepository, times(1)).merge(existing);
    }

    @Test
    void testUpdatePlanDuplicateNameFails() {
        UUID planId = UUID.randomUUID();
        UUID otherPlanId = UUID.randomUUID();
        PlanEntity existing = new PlanEntity(planId, "Old Name", "Old Desc", BigDecimal.valueOf(1000), 30, true);
        PlanEntity other = new PlanEntity(otherPlanId, "Existing Name", "Desc", BigDecimal.valueOf(1000), 30, true);
        when(planRepository.get(planId)).thenReturn(existing);
        when(planRepository.findByName("Existing Name")).thenReturn(Optional.of(other));

        com.freelance.gym.plan.dto.PlanUpdateRequestDTO request = new com.freelance.gym.plan.dto.PlanUpdateRequestDTO(
                planId,
                "Existing Name",
                "New Desc",
                BigDecimal.valueOf(1500),
                45,
                true
        );

        assertThrows(ConstraintException.class, () -> planService.updatePlan(request));
    }

    @Test
    void testUpdatePlanNotFoundFails() {
        UUID planId = UUID.randomUUID();
        when(planRepository.get(planId)).thenReturn(null);

        com.freelance.gym.plan.dto.PlanUpdateRequestDTO request = new com.freelance.gym.plan.dto.PlanUpdateRequestDTO(
                planId,
                "Some Name",
                "New Desc",
                BigDecimal.valueOf(1500),
                45,
                true
        );

        assertThrows(ConstraintException.class, () -> planService.updatePlan(request));
    }
}
