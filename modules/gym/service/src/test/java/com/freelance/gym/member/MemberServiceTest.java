package com.freelance.gym.member;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.UserContactDTO;
import com.freelance.alerts.preference.service.UserContactService;
import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.fixtures.TestContextHelper;
import com.freelance.gym.member.assembler.MemberAssembler;
import com.freelance.gym.member.dto.*;
import com.freelance.gym.payment.PaymentService;
import com.freelance.gym.payment.assembler.PaymentAssembler;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
import com.freelance.gym.plan.PlanService;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.gym.usertype.entity.UserTypeEntity;
import com.freelance.gym.usertype.entity.UserUserTypeEntity;
import com.freelance.gym.usertype.repository.UserTypeRepository;
import com.freelance.gym.usertype.repository.UserUserTypeRepository;
import com.freelance.identityManagement.PhoneNumber;
import com.freelance.user.UserDTO;
import com.freelance.user.create.UserCreateResponseDTO;
import com.freelance.user.create.UserCreateService;
import com.freelance.user.read.UserReadService;
import com.freelance.user.update.UserUpdateRequestDTO;
import com.freelance.user.update.UserUpdateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private UserReadService userReadService;

    @Mock
    private UserCreateService userCreateService;

    @Mock
    private UserUpdateService userUpdateService;

    @Mock
    private UserContactService userContactService;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private UserUserTypeRepository userUserTypeRepository;

    @Mock
    private UserPlanRepository userPlanRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserPlanPaymentRepository userPlanPaymentRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanService planService;

    @Mock
    private PaymentService paymentService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        memberService = new MemberService(
                userReadService,
                userCreateService,
                userUpdateService,
                userContactService,
                userTypeRepository,
                userUserTypeRepository,
                userPlanRepository,
                paymentRepository,
                userPlanPaymentRepository,
                planRepository,
                planService,
                paymentService,
                new MemberAssembler(),
                new PaymentAssembler()
        );
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new MemberService());
    }

    @Test
    void testCreateMemberProspectOnly() {
        UUID userId = UUID.randomUUID();
        UUID userTypeId = UUID.randomUUID();

        when(userTypeRepository.findByCode("MEMBER"))
                .thenReturn(Optional.of(new UserTypeEntity(userTypeId, "MEMBER", "Gym Member")));
        when(userCreateService.createUser(any())).thenReturn(new UserCreateResponseDTO(userId, "mem_9876543210"));

        MemberCreateRequestDTO request = new MemberCreateRequestDTO(
                "John",
                "Doe",
                new PhoneNumber("91", "9876543210"),
                List.of(),
                List.of()
        );

        MemberCreateResponseDTO response = memberService.createMember(request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals("mem_9876543210", response.username());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertNotNull(response.generatedPassword());
        assertEquals(List.of("MEMBER"), response.userTypes());
        assertTrue(response.plans().isEmpty());
        assertTrue(response.payments().isEmpty());

        verify(userUserTypeRepository, times(1)).persist(any(UserUserTypeEntity.class));
        verifyNoInteractions(planService);
        verifyNoInteractions(paymentService);
    }

    @Test
    void testCreateMember_MemberTypeMissing() {
        UUID userId = UUID.randomUUID();

        when(userTypeRepository.findByCode("MEMBER")).thenReturn(Optional.empty());
        when(userCreateService.createUser(any())).thenReturn(new UserCreateResponseDTO(userId, "mem_9876543210"));

        MemberCreateRequestDTO request = new MemberCreateRequestDTO(
                "John",
                "Doe",
                new PhoneNumber("91", "9876543210"),
                List.of(),
                List.of()
        );

        MemberCreateResponseDTO response = memberService.createMember(request);
        assertNotNull(response);
        verify(userUserTypeRepository, never()).persist(any());
    }

    @Test
    void testCreateMemberWithPlansAndPayments() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID userTypeId = UUID.randomUUID();

        when(userTypeRepository.findByCode("MEMBER"))
                .thenReturn(Optional.of(new UserTypeEntity(userTypeId, "MEMBER", "Gym Member")));
        when(userCreateService.createUser(any())).thenReturn(new UserCreateResponseDTO(userId, "mem_9876543210"));

        List<MemberPlanAssignmentDTO> plans = List.of(
                new MemberPlanAssignmentDTO(planId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        );

        List<PaymentScheduleDTO> payments = List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), PaymentMethod.UPI, "TXN_123", "Full payment")
        );

        UserPlanResponseDTO assignedPlan = new UserPlanResponseDTO(UUID.randomUUID(), userId, planId, "Gym Floor", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        when(planService.assignPlansToUser(eq(userId), eq(plans))).thenReturn(List.of(assignedPlan));

        PaymentResponseDTO assignedPayment = new PaymentResponseDTO(UUID.randomUUID(), BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), PaymentMethod.UPI, "TXN_123", "Full payment", List.of(assignedPlan));
        when(paymentService.createInstallments(eq(List.of(assignedPlan)), eq(payments))).thenReturn(List.of(assignedPayment));

        MemberCreateRequestDTO request = new MemberCreateRequestDTO(
                "Jane",
                "Doe",
                new PhoneNumber("91", "9876543210"),
                plans,
                payments
        );
        MemberCreateResponseDTO response = memberService.createMember(request);

        assertNotNull(response);
        assertEquals(1, response.plans().size());
        assertEquals(1, response.payments().size());
        assertEquals("Jane", response.firstName());
        assertEquals("Doe", response.lastName());
        verify(planService, times(1)).assignPlansToUser(userId, plans);
        verify(paymentService, times(1)).createInstallments(List.of(assignedPlan), payments);
    }

    @Test
    void testCreateMember_WithPlansNoPayments() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID userTypeId = UUID.randomUUID();

        when(userTypeRepository.findByCode("MEMBER"))
                .thenReturn(Optional.of(new UserTypeEntity(userTypeId, "MEMBER", "Gym Member")));
        when(userCreateService.createUser(any())).thenReturn(new UserCreateResponseDTO(userId, "mem_9876543210"));

        List<MemberPlanAssignmentDTO> plans = List.of(
                new MemberPlanAssignmentDTO(planId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        );

        UserPlanResponseDTO assignedPlan = new UserPlanResponseDTO(UUID.randomUUID(), userId, planId, "Gym Floor", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        when(planService.assignPlansToUser(eq(userId), eq(plans))).thenReturn(List.of(assignedPlan));

        MemberCreateRequestDTO request = new MemberCreateRequestDTO(
                "Jane",
                "Doe",
                new PhoneNumber("91", "9876543210"),
                plans,
                null
        );

        MemberCreateResponseDTO response = memberService.createMember(request);
        assertNotNull(response);
        assertEquals(1, response.plans().size());
        assertTrue(response.payments().isEmpty());
        verify(planService, times(1)).assignPlansToUser(userId, plans);
        verifyNoInteractions(paymentService);
    }

    @Test
    void testCreateMemberDuplicatePhoneFailsConstraint() {
        when(userReadService.existsByUsername("mem_9876543210")).thenReturn(true);

        MemberCreateRequestDTO request = new MemberCreateRequestDTO(
                "Duplicate",
                "User",
                new PhoneNumber("91", "9876543210"),
                List.of(),
                List.of()
        );

        assertThrows(com.freelance.rest.app.service.exception.constraint.ConstraintException.class, () -> memberService.createMember(request));
    }

    @Test
    void testRenewMemberSuccessAdditive() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        when(userReadService.existsById(userId)).thenReturn(true);

        UserPlanEntity existingPlan = new UserPlanEntity(UUID.randomUUID(), userId, planId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.of(existingPlan));

        // Request renewal without explicit startDate -> should auto-compute 2026-02-01 (old endDate + 1)
        List<MemberPlanAssignmentDTO> renewalPlans = List.of(
                new MemberPlanAssignmentDTO(planId, null, LocalDate.of(2026, 2, 28))
        );

        List<PaymentScheduleDTO> renewalPayments = List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), PaymentMethod.CASH, "TXN_RENEW", "Renewal payment")
        );

        UserPlanResponseDTO renewedPlan = new UserPlanResponseDTO(UUID.randomUUID(), userId, planId, "Gym Floor", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        when(planService.assignPlansToUser(eq(userId), any())).thenReturn(List.of(renewedPlan));

        PaymentResponseDTO renewedPayment = new PaymentResponseDTO(UUID.randomUUID(), BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1), PaymentMethod.CASH, "TXN_RENEW", "Renewal payment", List.of(renewedPlan));
        when(paymentService.createInstallments(eq(List.of(renewedPlan)), eq(renewalPayments))).thenReturn(List.of(renewedPayment));

        MemberRenewRequestDTO request = new MemberRenewRequestDTO(userId, renewalPlans, renewalPayments);
        MemberRenewResponseDTO response = memberService.renewMember(request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(1, response.renewedPlans().size());
        assertEquals(LocalDate.of(2026, 2, 1), response.renewedPlans().get(0).startDate());

        // Verify that NO deletions occurred! Strictly additive
        verify(userPlanRepository, never()).deleteByUserId(any());
        verify(paymentService, never()).deletePendingPaymentsByUserPlanIds(any());
    }

    @Test
    void testRenewMember_ExplicitStartDate() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        when(userReadService.existsById(userId)).thenReturn(true);
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.empty());

        // Explicit startDate provided in request
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 31);
        List<MemberPlanAssignmentDTO> renewalPlans = List.of(
                new MemberPlanAssignmentDTO(planId, start, end)
        );

        UserPlanResponseDTO renewedPlan = new UserPlanResponseDTO(UUID.randomUUID(), userId, planId, "Gym Floor", start, end);
        when(planService.assignPlansToUser(eq(userId), any())).thenReturn(List.of(renewedPlan));

        MemberRenewRequestDTO request = new MemberRenewRequestDTO(userId, renewalPlans, List.of());
        MemberRenewResponseDTO response = memberService.renewMember(request);

        assertNotNull(response);
        assertEquals(start, response.renewedPlans().get(0).startDate());
    }

    @Test
    void testRenewMember_NoPreviousPlanAndNoPayments() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        when(userReadService.existsById(userId)).thenReturn(true);
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.empty());

        // Request renewal without explicit startDate and no previous plan -> defaults to LocalDate.now()
        List<MemberPlanAssignmentDTO> renewalPlans = List.of(
                new MemberPlanAssignmentDTO(planId, null, LocalDate.now().plusDays(30))
        );

        UserPlanResponseDTO renewedPlan = new UserPlanResponseDTO(UUID.randomUUID(), userId, planId, "Gym Floor", LocalDate.now(), LocalDate.now().plusDays(30));
        when(planService.assignPlansToUser(eq(userId), any())).thenReturn(List.of(renewedPlan));

        MemberRenewRequestDTO request = new MemberRenewRequestDTO(userId, renewalPlans, null);
        MemberRenewResponseDTO response = memberService.renewMember(request);

        assertNotNull(response);
        assertTrue(response.newPayments().isEmpty());
        verifyNoInteractions(paymentService);
    }

    @Test
    void testRenewMember_PreviousPlanNullEndDate() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        when(userReadService.existsById(userId)).thenReturn(true);
        UserPlanEntity previousPlan = new UserPlanEntity(UUID.randomUUID(), userId, planId, LocalDate.of(2026, 1, 1), null);
        when(userPlanRepository.findLatestByUserAndPlan(userId, planId)).thenReturn(Optional.of(previousPlan));

        List<MemberPlanAssignmentDTO> renewalPlans = List.of(
                new MemberPlanAssignmentDTO(planId, null, LocalDate.now().plusDays(30))
        );

        UserPlanResponseDTO renewedPlan = new UserPlanResponseDTO(UUID.randomUUID(), userId, planId, "Gym Floor", LocalDate.now(), LocalDate.now().plusDays(30));
        when(planService.assignPlansToUser(eq(userId), any())).thenReturn(List.of(renewedPlan));

        MemberRenewRequestDTO request = new MemberRenewRequestDTO(userId, renewalPlans, List.of());
        MemberRenewResponseDTO response = memberService.renewMember(request);

        assertNotNull(response);
        assertTrue(response.newPayments().isEmpty());
    }

    @Test
    void testUpdateMemberDeleteInsertFlow() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID oldPlanId = UUID.randomUUID();

        when(userReadService.existsById(userId)).thenReturn(true);

        UserPlanEntity oldUserPlan = new UserPlanEntity(oldPlanId, userId, planId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        when(userPlanRepository.findByUserId(userId)).thenReturn(List.of(oldUserPlan));

        List<MemberPlanAssignmentDTO> updatedPlans = List.of(
                new MemberPlanAssignmentDTO(planId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        );

        List<PaymentScheduleDTO> updatedPayments = List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1200), BigDecimal.valueOf(1200), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), PaymentMethod.UPI, "TXN_UPDATED", "Updated payment")
        );

        UserPlanResponseDTO updatedPlanResp = new UserPlanResponseDTO(UUID.randomUUID(), userId, planId, "Gym Floor VIP", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        when(planService.assignPlansToUser(eq(userId), eq(updatedPlans))).thenReturn(List.of(updatedPlanResp));

        PaymentResponseDTO updatedPaymentResp = new PaymentResponseDTO(UUID.randomUUID(), BigDecimal.valueOf(1200), BigDecimal.valueOf(1200), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), PaymentMethod.UPI, "TXN_UPDATED", "Updated payment", List.of(updatedPlanResp));
        when(paymentService.createInstallments(eq(List.of(updatedPlanResp)), eq(updatedPayments))).thenReturn(List.of(updatedPaymentResp));

        MemberUpdateRequestDTO request = new MemberUpdateRequestDTO(
                userId,
                new MemberProfileDTO("John", "Updated", new PhoneNumber("91", "9876543210")),
                updatedPlans,
                updatedPayments
        );

        MemberUpdateResponseDTO response = memberService.updateMember(request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals(1, response.plans().size());

        // Verify user update service call
        verify(userUpdateService, times(1)).updateUser(any());

        // Verify Delete-Insert reconciliation
        verify(paymentService, times(1)).deletePendingPaymentsByUserPlanIds(List.of(oldPlanId));
        verify(userPlanRepository, times(1)).deleteByUserId(userId);
        verify(paymentService, times(1)).relinkPaidPayments(List.of(oldPlanId), List.of(updatedPlanResp));
    }

    @Test
    void testUpdateMember_NullProfileAndEmptyPlansPayments() {
        UUID userId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(true);
        when(userPlanRepository.findByUserId(userId)).thenReturn(List.of());

        MemberUpdateRequestDTO request = new MemberUpdateRequestDTO(
                userId,
                null,
                null,
                null
        );

        MemberUpdateResponseDTO response = memberService.updateMember(request);
        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertTrue(response.plans().isEmpty());
        assertTrue(response.payments().isEmpty());
        verifyNoInteractions(userUpdateService);
    }

    @Test
    void testUpdateMember_ProfileWithNullPhone() {
        UUID userId = UUID.randomUUID();
        when(userReadService.existsById(userId)).thenReturn(true);
        when(userPlanRepository.findByUserId(userId)).thenReturn(List.of());

        MemberUpdateRequestDTO request = new MemberUpdateRequestDTO(
                userId,
                new MemberProfileDTO("NewFirst", "NewLast", null),
                List.of(),
                List.of()
        );

        MemberUpdateResponseDTO response = memberService.updateMember(request);
        assertNotNull(response);

        ArgumentCaptor<UserUpdateRequestDTO> captor = ArgumentCaptor.forClass(UserUpdateRequestDTO.class);
        verify(userUpdateService).updateUser(captor.capture());
        assertNull(captor.getValue().contacts());
        assertEquals("NewFirst", captor.getValue().firstName());
    }

    @Test
    void testListMembersAggregatesRelationalData() {
        UUID userId = UUID.randomUUID();
        UserDTO user = new UserDTO(userId, "mem_user", "hash", "John", "Doe", LocalDateTime.now(), LocalDateTime.now());

        UUID userTypeId = UUID.randomUUID();
        UserTypeEntity typeEntity = new UserTypeEntity(userTypeId, "MEMBER", "Member");

        UUID planId = UUID.randomUUID();
        PlanEntity plan = new PlanEntity(planId, "Floor Access", "", BigDecimal.valueOf(1000), 30, true);
        UUID userPlanId = UUID.randomUUID();
        UserPlanEntity up = new UserPlanEntity(userPlanId, userId, planId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        UUID paymentId = UUID.randomUUID();
        PaymentEntity payment = new PaymentEntity(paymentId, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), PaymentMethod.UPI, "TXN_PAID", "Paid");
        UserPlanPaymentEntity mapping = new UserPlanPaymentEntity(userPlanId, paymentId);

        UserContactDTO contact = new UserContactDTO(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210", false, true);

        when(userTypeRepository.findByCode("MEMBER")).thenReturn(Optional.of(typeEntity));
        when(userUserTypeRepository.findUserIdsByUserTypeId(userTypeId)).thenReturn(List.of(userId));
        when(userReadService.readUsersByIds(List.of(userId))).thenReturn(List.of(user));
        when(userPlanRepository.findByUserIds(List.of(userId))).thenReturn(List.of(up));
        when(planRepository.findAllPlans()).thenReturn(List.of(plan));
        when(userPlanPaymentRepository.findByUserPlanIds(List.of(userPlanId))).thenReturn(List.of(mapping));
        when(paymentRepository.findByIds(List.of(paymentId))).thenReturn(List.of(payment));
        when(userContactService.findByUserIds(List.of(userId))).thenReturn(List.of(contact));

        MemberListResponseDTO response = memberService.listMembers();

        assertNotNull(response);
        assertEquals(1, response.members().size());
        MemberRecordDTO record = response.members().get(0);
        assertEquals(userId, record.userId());
        assertEquals("mem_user", record.username());
        assertEquals("John", record.firstName());
        assertEquals("Doe", record.lastName());
        assertEquals(1, record.payments().size());
        assertEquals(1, record.payments().get(0).plans().size());
        assertEquals("Floor Access", record.payments().get(0).plans().get(0).planName());
    }

    @Test
    void testListMembers_EmptyCases() {
        // Case 1: Member type missing
        when(userTypeRepository.findByCode("MEMBER")).thenReturn(Optional.empty());
        assertTrue(memberService.listMembers().members().isEmpty());

        // Case 2: Member user IDs empty
        UUID userTypeId = UUID.randomUUID();
        when(userTypeRepository.findByCode("MEMBER")).thenReturn(Optional.of(new UserTypeEntity(userTypeId, "MEMBER", "Member")));
        when(userUserTypeRepository.findUserIdsByUserTypeId(userTypeId)).thenReturn(List.of());
        assertTrue(memberService.listMembers().members().isEmpty());

        // Case 3: Users read from ID empty
        UUID userId = UUID.randomUUID();
        when(userUserTypeRepository.findUserIdsByUserTypeId(userTypeId)).thenReturn(List.of(userId));
        when(userReadService.readUsersByIds(List.of(userId))).thenReturn(List.of());
        assertTrue(memberService.listMembers().members().isEmpty());
    }

    @Test
    void testListMembers_NoUserPlansAndPhoneParsingCases() {
        UUID userId = UUID.randomUUID();
        UserDTO user = new UserDTO(userId, "mem_user", "hash", "John", "Doe", LocalDateTime.now(), LocalDateTime.now());

        UUID userTypeId = UUID.randomUUID();
        when(userTypeRepository.findByCode("MEMBER")).thenReturn(Optional.of(new UserTypeEntity(userTypeId, "MEMBER", "Member")));
        when(userUserTypeRepository.findUserIdsByUserTypeId(userTypeId)).thenReturn(List.of(userId));
        when(userReadService.readUsersByIds(List.of(userId))).thenReturn(List.of(user));

        // No user plans
        when(userPlanRepository.findByUserIds(List.of(userId))).thenReturn(List.of());
        when(planRepository.findAllPlans()).thenReturn(List.of());

        // Contacts with variations: non-whatsapp, whatsapp with null value, whatsapp with short national number (<=10)
        List<UserContactDTO> contacts = List.of(
                new UserContactDTO(UUID.randomUUID(), userId, AlertChannel.PUSH, "token123", false, true),
                new UserContactDTO(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, null, false, true),
                new UserContactDTO(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "9876543210", false, true)
        );
        when(userContactService.findByUserIds(List.of(userId))).thenReturn(contacts);

        MemberListResponseDTO response = memberService.listMembers();
        assertNotNull(response);
        assertEquals(1, response.members().size());
        MemberRecordDTO record = response.members().get(0);
        assertNotNull(record.phoneNumber());
        assertEquals("91", record.phoneNumber().countryCode());
        assertEquals("9876543210", record.phoneNumber().number());
        assertTrue(record.payments().isEmpty());
    }

    @Test
    void testListMembers_DuplicateMappingsAndNullRefsAndBlankContacts() {
        UUID userId = UUID.randomUUID();
        UserDTO user = new UserDTO(userId, "mem_user", "hash", "John", "Doe", LocalDateTime.now(), LocalDateTime.now());

        UUID userTypeId = UUID.randomUUID();
        when(userTypeRepository.findByCode("MEMBER")).thenReturn(Optional.of(new UserTypeEntity(userTypeId, "MEMBER", "Member")));
        when(userUserTypeRepository.findUserIdsByUserTypeId(userTypeId)).thenReturn(List.of(userId));
        when(userReadService.readUsersByIds(List.of(userId))).thenReturn(List.of(user));

        UUID planId = UUID.randomUUID();
        PlanEntity plan = new PlanEntity(planId, "Floor Access", "", BigDecimal.valueOf(1000), 30, true);
        UUID userPlanId1 = UUID.randomUUID();
        UUID userPlanId2 = UUID.randomUUID();
        UserPlanEntity up1 = new UserPlanEntity(userPlanId1, userId, planId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        UserPlanEntity up2 = new UserPlanEntity(userPlanId2, userId, planId, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        UUID paymentId1 = UUID.randomUUID();
        UUID unmappedPlanId = UUID.randomUUID();
        UUID missingPaymentId = UUID.randomUUID();

        PaymentEntity payment1 = new PaymentEntity(paymentId1, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), PaymentMethod.UPI, "TXN_PAID", "Paid");

        // Duplicate mapping for payment1 (maps to both up1 and up2 for the same user)
        UserPlanPaymentEntity mapping1 = new UserPlanPaymentEntity(userPlanId1, paymentId1);
        UserPlanPaymentEntity mapping2 = new UserPlanPaymentEntity(userPlanId2, paymentId1);
        // Mapping with unmapped user plan ID (uid == null)
        UserPlanPaymentEntity mapping3 = new UserPlanPaymentEntity(unmappedPlanId, paymentId1);
        // Mapping with missing payment in payment repository (p == null)
        UserPlanPaymentEntity mapping4 = new UserPlanPaymentEntity(userPlanId1, missingPaymentId);

        when(userPlanRepository.findByUserIds(List.of(userId))).thenReturn(List.of(up1, up2));
        when(planRepository.findAllPlans()).thenReturn(List.of(plan));
        when(userPlanPaymentRepository.findByUserPlanIds(List.of(userPlanId1, userPlanId2))).thenReturn(List.of(mapping1, mapping2, mapping3, mapping4));
        when(paymentRepository.findByIds(anyList())).thenReturn(List.of(payment1));

        // Contacts with blank contact value
        List<UserContactDTO> contacts = List.of(
                new UserContactDTO(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "   ", false, true)
        );
        when(userContactService.findByUserIds(List.of(userId))).thenReturn(contacts);

        MemberListResponseDTO response = memberService.listMembers();
        assertNotNull(response);
        assertEquals(1, response.members().size());
        MemberRecordDTO record = response.members().get(0);
        assertNull(record.phoneNumber());
        // Only 1 payment even though mapped twice
        assertEquals(1, record.payments().size());
    }
}
