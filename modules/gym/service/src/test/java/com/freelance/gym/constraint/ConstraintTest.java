package com.freelance.gym.constraint;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.UserContactDTO;
import com.freelance.alerts.preference.service.UserContactService;
import com.freelance.gym.fixtures.TestContextHelper;
import com.freelance.gym.member.constraint.MemberCreateConstraint;
import com.freelance.gym.member.constraint.MemberRenewConstraint;
import com.freelance.gym.member.constraint.MemberUpdateConstraint;
import com.freelance.gym.member.dto.MemberCreateRequestDTO;
import com.freelance.gym.member.dto.MemberPlanAssignmentDTO;
import com.freelance.gym.member.dto.MemberRenewRequestDTO;
import com.freelance.gym.member.dto.MemberUpdateRequestDTO;
import com.freelance.gym.payment.constraint.PaymentScheduleConstraint;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.plan.constraint.PlanCreateConstraint;
import com.freelance.gym.plan.constraint.PlanDeleteConstraint;
import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.identityManagement.PhoneNumber;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.user.read.UserReadService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConstraintTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserReadService userReadService;

    @Mock
    private UserContactService userContactService;

    @Mock
    private UserPlanRepository userPlanRepository;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    // ==========================================
    // PaymentScheduleConstraint Tests
    // ==========================================
    @Test
    void testPaymentScheduleConstraint_DefaultConstructor() {
        assertDoesNotThrow(() -> new PaymentScheduleConstraint());
    }

    @Test
    void testPaymentScheduleConstraint_NullOrEmpty() {
        PaymentScheduleConstraint constraint = new PaymentScheduleConstraint(paymentRepository);
        assertDoesNotThrow(() -> constraint.checkPaidPaymentsImmutable(null));
        assertDoesNotThrow(() -> constraint.checkPaidPaymentsImmutable(List.of()));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void testPaymentScheduleConstraint_UnpaidPayments() {
        PaymentScheduleConstraint constraint = new PaymentScheduleConstraint(paymentRepository);
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        PaymentEntity unpaid1 = new PaymentEntity();
        unpaid1.setId(p1);
        unpaid1.setTransactionId(null);

        PaymentEntity unpaid2 = new PaymentEntity();
        unpaid2.setId(p2);
        unpaid2.setTransactionId("   ");

        when(paymentRepository.findByIds(List.of(p1, p2))).thenReturn(List.of(unpaid1, unpaid2));

        assertDoesNotThrow(() -> constraint.checkPaidPaymentsImmutable(List.of(p1, p2)));
    }

    @Test
    void testPaymentScheduleConstraint_PaidPaymentCannotBeModified() {
        PaymentScheduleConstraint constraint = new PaymentScheduleConstraint(paymentRepository);
        UUID p1 = UUID.randomUUID();

        PaymentEntity paid = new PaymentEntity();
        paid.setId(p1);
        paid.setTransactionId("TXN_PAID_123");

        when(paymentRepository.findByIds(List.of(p1))).thenReturn(List.of(paid));

        assertThrows(ConstraintException.class, () -> constraint.checkPaidPaymentsImmutable(List.of(p1)));
    }

    // ==========================================
    // PlanCreateConstraint Tests
    // ==========================================
    @Test
    void testPlanCreateConstraint_DefaultConstructor() {
        assertDoesNotThrow(() -> new PlanCreateConstraint());
    }

    @Test
    void testPlanCreateConstraint_NullRequest() {
        PlanCreateConstraint constraint = new PlanCreateConstraint(planRepository);
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
        verifyNoInteractions(planRepository);
    }

    @Test
    void testPlanCreateConstraint_UniqueName() {
        PlanCreateConstraint constraint = new PlanCreateConstraint(planRepository);
        PlanCreateRequestDTO req = new PlanCreateRequestDTO("Gold", "desc", BigDecimal.valueOf(100), 30);
        when(planRepository.findByName("Gold")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    void testPlanCreateConstraint_DuplicateName() {
        PlanCreateConstraint constraint = new PlanCreateConstraint(planRepository);
        PlanCreateRequestDTO req = new PlanCreateRequestDTO("Gold", "desc", BigDecimal.valueOf(100), 30);
        PlanEntity existing = new PlanEntity();
        existing.setName("Gold");
        when(planRepository.findByName("Gold")).thenReturn(Optional.of(existing));

        assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
    }

    // ==========================================
    // PlanDeleteConstraint Tests
    // ==========================================
    @Test
    void testPlanDeleteConstraint_DefaultConstructor() {
        assertDoesNotThrow(() -> new PlanDeleteConstraint());
    }

    @Test
    void testPlanDeleteConstraint_NullRequestOrPlanId() {
        PlanDeleteConstraint constraint = new PlanDeleteConstraint(planRepository);
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
        assertDoesNotThrow(() -> constraint.checkConstraints(new PlanDeleteRequestDTO(null)));
        verifyNoInteractions(planRepository);
    }

    @Test
    void testPlanDeleteConstraint_PlanFound() {
        PlanDeleteConstraint constraint = new PlanDeleteConstraint(planRepository);
        UUID planId = UUID.randomUUID();
        when(planRepository.get(planId)).thenReturn(new PlanEntity());

        assertDoesNotThrow(() -> constraint.checkConstraints(new PlanDeleteRequestDTO(planId)));
    }

    @Test
    void testPlanDeleteConstraint_PlanNotFound() {
        PlanDeleteConstraint constraint = new PlanDeleteConstraint(planRepository);
        UUID planId = UUID.randomUUID();
        when(planRepository.get(planId)).thenReturn(null);

        assertThrows(ConstraintException.class, () -> constraint.checkConstraints(new PlanDeleteRequestDTO(planId)));
    }

    // ==========================================
    // MemberCreateConstraint Tests
    // ==========================================
    @Test
    void testMemberCreateConstraint_DefaultConstructor() {
        assertDoesNotThrow(() -> new MemberCreateConstraint());
    }

    @Test
    void testMemberCreateConstraint_NullRequestOrPhone() {
        MemberCreateConstraint constraint = new MemberCreateConstraint(userReadService, userContactService);
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
        assertDoesNotThrow(() -> constraint.checkConstraints(new MemberCreateRequestDTO("John", "Doe", null, null, null)));
        verifyNoInteractions(userReadService);
    }

    @Test
    void testMemberCreateConstraint_UsernameExists() {
        MemberCreateConstraint constraint = new MemberCreateConstraint(userReadService, userContactService);
        PhoneNumber phone = new PhoneNumber("91", "9876543210");
        when(userReadService.existsByUsername("mem_9876543210")).thenReturn(true);

        assertThrows(ConstraintException.class, () -> constraint.checkConstraints(
                new MemberCreateRequestDTO("John", "Doe", phone, null, null)
        ));
    }

    @Test
    void testMemberCreateConstraint_WhatsAppContactExists() {
        MemberCreateConstraint constraint = new MemberCreateConstraint(userReadService, userContactService);
        PhoneNumber phone = new PhoneNumber("91", "9876543210");
        when(userReadService.existsByUsername("mem_9876543210")).thenReturn(false);
        when(userContactService.findByContactAndChannel(phone.toE164(), AlertChannel.WHATSAPP))
                .thenReturn(Optional.of(new UserContactDTO(UUID.randomUUID(), UUID.randomUUID(), AlertChannel.WHATSAPP, phone.toE164(), true, true)));

        assertThrows(ConstraintException.class, () -> constraint.checkConstraints(
                new MemberCreateRequestDTO("John", "Doe", phone, null, null)
        ));
    }

    @Test
    void testMemberCreateConstraint_Valid() {
        MemberCreateConstraint constraint = new MemberCreateConstraint(userReadService, userContactService);
        PhoneNumber phone = new PhoneNumber("91", "9876543210");
        when(userReadService.existsByUsername("mem_9876543210")).thenReturn(false);
        when(userContactService.findByContactAndChannel(phone.toE164(), AlertChannel.WHATSAPP))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> constraint.checkConstraints(
                new MemberCreateRequestDTO("John", "Doe", phone, null, null)
        ));
    }

    @Test
    void testMemberCreateConstraint_NullToE164() {
        PhoneNumber mockPhone = mock(PhoneNumber.class);
        when(mockPhone.number()).thenReturn("9876543210");
        when(mockPhone.toE164()).thenReturn(null);
        when(userReadService.existsByUsername("mem_9876543210")).thenReturn(false);

        MemberCreateConstraint constraint = new MemberCreateConstraint(userReadService, userContactService);
        assertDoesNotThrow(() -> constraint.checkConstraints(
                new MemberCreateRequestDTO("John", "Doe", mockPhone, null, null)
        ));
    }

    @Test
    void testMemberCreateConstraint_NullServices() {
        MemberCreateConstraint constraint = new MemberCreateConstraint(null, null);
        PhoneNumber phone = new PhoneNumber("91", "9876543210");
        assertDoesNotThrow(() -> constraint.checkConstraints(
                new MemberCreateRequestDTO("John", "Doe", phone, null, null)
        ));
    }

    // ==========================================
    // MemberRenewConstraint Tests
    // ==========================================
    @Test
    void testMemberRenewConstraint_DefaultConstructor() {
        assertDoesNotThrow(() -> new MemberRenewConstraint());
    }

    @Test
    void testMemberRenewConstraint_NullRequestOrUserId() {
        MemberRenewConstraint constraint = new MemberRenewConstraint(userReadService, userPlanRepository);
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
        assertDoesNotThrow(() -> constraint.checkConstraints(new MemberRenewRequestDTO(null, null, null)));
        verifyNoInteractions(userReadService);
    }

    @Test
    void testMemberRenewConstraint_MemberNotFound() {
        MemberRenewConstraint constraint = new MemberRenewConstraint(userReadService, userPlanRepository);
        UUID userId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(false);

        assertThrows(ConstraintException.class, () -> constraint.checkConstraints(
                new MemberRenewRequestDTO(userId, List.of(), List.of())
        ));
    }

    @Test
    void testMemberRenewConstraint_DateOverlapsExisting() {
        MemberRenewConstraint constraint = new MemberRenewConstraint(userReadService, userPlanRepository);
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(true);

        UserPlanEntity previousPlan = new UserPlanEntity();
        previousPlan.setEndDate(LocalDate.of(2026, 10, 1));
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.of(previousPlan));

        // Start date is equal or before previousEnd
        MemberRenewRequestDTO req = new MemberRenewRequestDTO(
                userId,
                List.of(new MemberPlanAssignmentDTO(planId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))),
                List.of()
        );

        assertThrows(ConstraintException.class, () -> constraint.checkConstraints(req));
    }

    @Test
    void testMemberRenewConstraint_ValidRenewals() {
        MemberRenewConstraint constraint = new MemberRenewConstraint(userReadService, userPlanRepository);
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(true);

        UserPlanEntity previousPlan = new UserPlanEntity();
        previousPlan.setEndDate(LocalDate.of(2026, 10, 1));
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.of(previousPlan));

        // Start date strictly after previousEnd
        MemberRenewRequestDTO req = new MemberRenewRequestDTO(
                userId,
                List.of(new MemberPlanAssignmentDTO(planId, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 11, 2))),
                List.of()
        );

        assertDoesNotThrow(() -> constraint.checkConstraints(req));

        // Plan with null previousEnd or no latest plan
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> constraint.checkConstraints(req));

        previousPlan.setEndDate(null);
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.of(previousPlan));
        assertDoesNotThrow(() -> constraint.checkConstraints(req));
    }

    @Test
    void testMemberRenewConstraint_NullPlansOrNullItems() {
        MemberRenewConstraint constraint = new MemberRenewConstraint(userReadService, userPlanRepository);
        UUID userId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(true);

        assertDoesNotThrow(() -> constraint.checkConstraints(new MemberRenewRequestDTO(userId, null, null)));

        List<MemberPlanAssignmentDTO> plansWithNulls = new java.util.ArrayList<>();
        plansWithNulls.add(null);
        plansWithNulls.add(new MemberPlanAssignmentDTO(null, LocalDate.now(), LocalDate.now()));
        plansWithNulls.add(new MemberPlanAssignmentDTO(UUID.randomUUID(), null, LocalDate.now()));
        assertDoesNotThrow(() -> constraint.checkConstraints(new MemberRenewRequestDTO(userId, plansWithNulls, null)));
    }

    @Test
    void testMemberRenewConstraint_NullServices() {
        MemberRenewConstraint constraint = new MemberRenewConstraint(null, null);
        UUID userId = UUID.randomUUID();
        assertDoesNotThrow(() -> constraint.checkConstraints(new MemberRenewRequestDTO(userId, null, null)));
    }

    // ==========================================
    // MemberUpdateConstraint Tests
    // ==========================================
    @Test
    void testMemberUpdateConstraint_DefaultConstructor() {
        assertDoesNotThrow(() -> new MemberUpdateConstraint());
    }

    @Test
    void testMemberUpdateConstraint_NullService() {
        MemberUpdateConstraint constraint = new MemberUpdateConstraint(null);
        UUID userId = UUID.randomUUID();
        assertDoesNotThrow(() -> constraint.checkConstraints(new MemberUpdateRequestDTO(userId, null, null, null)));
    }

    @Test
    void testMemberUpdateConstraint_NullRequestOrUserId() {
        MemberUpdateConstraint constraint = new MemberUpdateConstraint(userReadService);
        assertDoesNotThrow(() -> constraint.checkConstraints(null));
        assertDoesNotThrow(() -> constraint.checkConstraints(new MemberUpdateRequestDTO(null, null, null, null)));
        verifyNoInteractions(userReadService);
    }

    @Test
    void testMemberUpdateConstraint_MemberNotFound() {
        MemberUpdateConstraint constraint = new MemberUpdateConstraint(userReadService);
        UUID userId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(false);

        assertThrows(ConstraintException.class, () -> constraint.checkConstraints(
                new MemberUpdateRequestDTO(userId, null, null, null)
        ));
    }

    @Test
    void testMemberUpdateConstraint_Valid() {
        MemberUpdateConstraint constraint = new MemberUpdateConstraint(userReadService);
        UUID userId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(true);

        assertDoesNotThrow(() -> constraint.checkConstraints(
                new MemberUpdateRequestDTO(userId, null, null, null)
        ));
    }
}
