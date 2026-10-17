package com.freelance.gym.payment;

import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.fixtures.TestContextHelper;
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.assembler.PaymentAssembler;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserPlanPaymentRepository userPlanPaymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        paymentService = new PaymentService(paymentRepository, userPlanPaymentRepository, new PaymentAssembler());
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new PaymentService());
    }

    @Test
    void testCreateInstallmentsSuccess() {
        UUID userId = UUID.randomUUID();
        UUID userPlanId1 = UUID.randomUUID();
        UUID userPlanId2 = UUID.randomUUID();

        List<UserPlanResponseDTO> userPlans = List.of(
                new UserPlanResponseDTO(userPlanId1, userId, UUID.randomUUID(), "Gym Floor", LocalDate.now(), LocalDate.now().plusMonths(1)),
                new UserPlanResponseDTO(userPlanId2, userId, UUID.randomUUID(), "PT", LocalDate.now(), LocalDate.now().plusMonths(1))
        );

        List<PaymentScheduleDTO> payments = List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1500), BigDecimal.valueOf(1500), LocalDate.now(), LocalDate.now(), PaymentMethod.CASH, "TXN_12345", "First installment"),
                new PaymentScheduleDTO(BigDecimal.valueOf(1500), BigDecimal.ZERO, LocalDate.now().plusDays(15), null, PaymentMethod.UPI, null, "Second installment")
        );

        List<PaymentResponseDTO> response = paymentService.createInstallments(userPlans, payments);

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(BigDecimal.valueOf(1500), response.get(0).amount());
        assertEquals("TXN_12345", response.get(0).transactionId());
        assertNull(response.get(1).transactionId());
        assertEquals(2, response.get(0).plans().size());

        verify(paymentRepository, times(2)).persist(any(PaymentEntity.class));
        // 2 payments * 2 plans = 4 mappings
        verify(userPlanPaymentRepository, times(4)).persist(any(UserPlanPaymentEntity.class));
    }

    @Test
    void testCreateInstallmentsWithNullOrEmptyPlans() {
        List<PaymentScheduleDTO> payments = List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.ZERO, LocalDate.now(), null, null, null, null)
        );

        // When userPlans is null
        List<PaymentResponseDTO> res1 = paymentService.createInstallments(null, payments);
        assertEquals(1, res1.size());
        assertTrue(res1.get(0).plans().isEmpty());

        // When userPlans is empty
        List<PaymentResponseDTO> res2 = paymentService.createInstallments(List.of(), payments);
        assertEquals(1, res2.size());
        assertTrue(res2.get(0).plans().isEmpty());
    }

    @Test
    void testCreateInstallmentsWithNullOrEmptyPayments() {
        List<PaymentResponseDTO> res1 = paymentService.createInstallments(List.of(), null);
        assertTrue(res1.isEmpty());

        List<PaymentResponseDTO> res2 = paymentService.createInstallments(List.of(), List.of());
        assertTrue(res2.isEmpty());

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(userPlanPaymentRepository);
    }

    @Test
    void testCreateInstallmentsValidationFailsForInvalidAmount() {
        List<PaymentScheduleDTO> payments = List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(-100), BigDecimal.ZERO, LocalDate.now(), null, PaymentMethod.CASH, null, "Bad amount")
        );

        assertThrows(ValidationException.class, () -> paymentService.createInstallments(List.of(), payments));
    }

    @Test
    void testDeletePendingPaymentsByUserPlanIds() {
        UUID planId1 = UUID.randomUUID();
        UUID paymentId1 = UUID.randomUUID();
        UUID paymentId2 = UUID.randomUUID();

        UserPlanPaymentEntity m1 = new UserPlanPaymentEntity(planId1, paymentId1);
        UserPlanPaymentEntity m2 = new UserPlanPaymentEntity(planId1, paymentId2);

        when(userPlanPaymentRepository.findByUserPlanIds(List.of(planId1))).thenReturn(List.of(m1, m2));

        PaymentEntity p1 = new PaymentEntity(paymentId1, BigDecimal.valueOf(500), BigDecimal.ZERO, LocalDate.now(), null, PaymentMethod.CASH, null, null);
        PaymentEntity p2 = new PaymentEntity(paymentId2, BigDecimal.valueOf(500), BigDecimal.valueOf(500), LocalDate.now().plusDays(10), LocalDate.now(), PaymentMethod.CASH, "TXN_PAID", null);

        when(paymentRepository.findByIds(List.of(paymentId1, paymentId2))).thenReturn(List.of(p1, p2));

        paymentService.deletePendingPaymentsByUserPlanIds(List.of(planId1));

        verify(userPlanPaymentRepository, times(1)).deleteByPaymentIds(List.of(paymentId1));
        verify(paymentRepository, times(1)).deletePendingByIds(List.of(paymentId1));
    }

    @Test
    void testDeletePendingPaymentsByUserPlanIds_EdgeCases() {
        // Null or empty
        assertDoesNotThrow(() -> paymentService.deletePendingPaymentsByUserPlanIds(null));
        assertDoesNotThrow(() -> paymentService.deletePendingPaymentsByUserPlanIds(List.of()));

        // No mappings
        UUID planId = UUID.randomUUID();
        when(userPlanPaymentRepository.findByUserPlanIds(List.of(planId))).thenReturn(List.of());
        assertDoesNotThrow(() -> paymentService.deletePendingPaymentsByUserPlanIds(List.of(planId)));

        // Only paid payments (no pending)
        UUID paidPaymentId = UUID.randomUUID();
        UserPlanPaymentEntity m = new UserPlanPaymentEntity(planId, paidPaymentId);
        when(userPlanPaymentRepository.findByUserPlanIds(List.of(planId))).thenReturn(List.of(m));
        PaymentEntity paidPayment = new PaymentEntity(paidPaymentId, BigDecimal.valueOf(500), BigDecimal.valueOf(500), LocalDate.now(), LocalDate.now(), PaymentMethod.CASH, "TXN_PAID", null);
        when(paymentRepository.findByIds(List.of(paidPaymentId))).thenReturn(List.of(paidPayment));

        assertDoesNotThrow(() -> paymentService.deletePendingPaymentsByUserPlanIds(List.of(planId)));
        verify(paymentRepository, never()).deletePendingByIds(anyList());
    }

    @Test
    void testRelinkPaidPayments() {
        UUID oldPlanId = UUID.randomUUID();
        UUID newPlanId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        UserPlanPaymentEntity mapping = new UserPlanPaymentEntity(oldPlanId, paymentId);
        when(userPlanPaymentRepository.findByUserPlanIds(List.of(oldPlanId))).thenReturn(List.of(mapping));

        PaymentEntity paidPayment = new PaymentEntity(paymentId, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.now(), LocalDate.now(), PaymentMethod.UPI, "TXN_PAID", null);
        when(paymentRepository.findByIds(List.of(paymentId))).thenReturn(List.of(paidPayment));

        List<UserPlanResponseDTO> newPlans = List.of(
                new UserPlanResponseDTO(newPlanId, UUID.randomUUID(), UUID.randomUUID(), "Gym Floor", LocalDate.now(), LocalDate.now().plusMonths(1))
        );

        paymentService.relinkPaidPayments(List.of(oldPlanId), newPlans);

        verify(userPlanPaymentRepository, times(1)).persist(any(UserPlanPaymentEntity.class));
    }

    @Test
    void testRelinkPaidPayments_EdgeCases() {
        UUID oldPlanId = UUID.randomUUID();
        List<UserPlanResponseDTO> newPlans = List.of(
                new UserPlanResponseDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Gym Floor", LocalDate.now(), LocalDate.now().plusMonths(1))
        );

        // Null or empty combinations
        assertDoesNotThrow(() -> paymentService.relinkPaidPayments(null, newPlans));
        assertDoesNotThrow(() -> paymentService.relinkPaidPayments(List.of(), newPlans));
        assertDoesNotThrow(() -> paymentService.relinkPaidPayments(List.of(oldPlanId), null));
        assertDoesNotThrow(() -> paymentService.relinkPaidPayments(List.of(oldPlanId), List.of()));

        // No mappings
        when(userPlanPaymentRepository.findByUserPlanIds(List.of(oldPlanId))).thenReturn(List.of());
        assertDoesNotThrow(() -> paymentService.relinkPaidPayments(List.of(oldPlanId), newPlans));

        // No paid payments (only pending payments)
        UUID pendingPaymentId = UUID.randomUUID();
        UserPlanPaymentEntity mapping = new UserPlanPaymentEntity(oldPlanId, pendingPaymentId);
        when(userPlanPaymentRepository.findByUserPlanIds(List.of(oldPlanId))).thenReturn(List.of(mapping));

        PaymentEntity unpaidPayment = new PaymentEntity(pendingPaymentId, BigDecimal.valueOf(1000), BigDecimal.ZERO, LocalDate.now(), null, PaymentMethod.UPI, "   ", null);
        when(paymentRepository.findByIds(List.of(pendingPaymentId))).thenReturn(List.of(unpaidPayment));

        assertDoesNotThrow(() -> paymentService.relinkPaidPayments(List.of(oldPlanId), newPlans));
    }

    @Test
    void testCollectPaymentSuccess() {
        UUID paymentId = UUID.randomUUID();
        PaymentEntity pending = new PaymentEntity(paymentId, BigDecimal.valueOf(1000), BigDecimal.ZERO, LocalDate.now(), null, PaymentMethod.NONE, null, "Pending");
        when(paymentRepository.get(paymentId)).thenReturn(pending);
        when(userPlanPaymentRepository.findByPaymentId(paymentId)).thenReturn(List.of());

        com.freelance.gym.payment.dto.PaymentCollectRequestDTO request = new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                paymentId,
                BigDecimal.valueOf(1000),
                LocalDate.now(),
                PaymentMethod.UPI,
                "TXN_COLLECTED_01",
                "Collected at front desk"
        );

        PaymentResponseDTO response = paymentService.collectPayment(request);

        assertNotNull(response);
        assertEquals(paymentId, response.id());
        assertEquals(BigDecimal.valueOf(1000), response.paidAmount());
        assertEquals(PaymentMethod.UPI, response.paymentMethod());
        assertEquals("TXN_COLLECTED_01", response.transactionId());
        assertEquals("Collected at front desk", response.notes());

        verify(paymentRepository, times(1)).merge(pending);
    }

    @Test
    void testCreateInstallmentsWithPlanIndices() {
        UUID planId1 = UUID.randomUUID();
        UUID planId2 = UUID.randomUUID();
        List<UserPlanResponseDTO> userPlans = List.of(
                new UserPlanResponseDTO(planId1, UUID.randomUUID(), UUID.randomUUID(), "Gym", LocalDate.now(), LocalDate.now().plusMonths(1)),
                new UserPlanResponseDTO(planId2, UUID.randomUUID(), UUID.randomUUID(), "Zumba", LocalDate.now(), LocalDate.now().plusMonths(1))
        );

        PaymentScheduleDTO sched1 = new PaymentScheduleDTO(
                BigDecimal.valueOf(500),
                BigDecimal.ZERO,
                LocalDate.now(),
                null,
                PaymentMethod.UPI,
                null,
                "Zumba only",
                List.of(1)
        );

        List<PaymentResponseDTO> res = paymentService.createInstallments(userPlans, List.of(sched1));
        assertEquals(1, res.size());
        verify(paymentRepository, times(1)).persist(any(PaymentEntity.class));
        verify(userPlanPaymentRepository, times(1)).persist(argThat(upp -> upp.getUserPlanId().equals(planId2)));
    }

    @Test
    void testLinkPaidPaymentsPreciselyWithExplicitLinks() {
        UUID newPlanId1 = UUID.randomUUID();
        UUID newPlanId2 = UUID.randomUUID();
        List<UserPlanResponseDTO> newPlans = List.of(
                new UserPlanResponseDTO(newPlanId1, UUID.randomUUID(), UUID.randomUUID(), "Gym", LocalDate.now(), LocalDate.now().plusMonths(1)),
                new UserPlanResponseDTO(newPlanId2, UUID.randomUUID(), UUID.randomUUID(), "Zumba", LocalDate.now(), LocalDate.now().plusMonths(1))
        );

        UUID paidId = UUID.randomUUID();
        PaymentEntity paid = new PaymentEntity(paidId, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), LocalDate.now(), LocalDate.now(), PaymentMethod.UPI, "TXN_1", "Paid");

        var explicitLink = new com.freelance.gym.payment.dto.PaidPaymentPlanLinkDTO(paidId, List.of(0, 1));

        paymentService.linkPaidPaymentsPrecisely(
                List.of(paid),
                newPlans,
                List.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(explicitLink)
        );

        verify(userPlanPaymentRepository, times(1)).persist(argThat(upp -> upp.getUserPlanId().equals(newPlanId1) && upp.getPaymentId().equals(paidId)));
        verify(userPlanPaymentRepository, times(1)).persist(argThat(upp -> upp.getUserPlanId().equals(newPlanId2) && upp.getPaymentId().equals(paidId)));
    }
}
