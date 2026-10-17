package com.freelance.gym.validation;

import com.freelance.gym.fixtures.TestContextHelper;
import com.freelance.gym.member.dto.MemberCreateRequestDTO;
import com.freelance.gym.member.dto.MemberPlanAssignmentDTO;
import com.freelance.gym.member.dto.MemberProfileDTO;
import com.freelance.gym.member.dto.MemberRenewRequestDTO;
import com.freelance.gym.member.dto.MemberUpdateRequestDTO;
import com.freelance.gym.member.validation.MemberCreateValidation;
import com.freelance.gym.member.validation.MemberRenewValidation;
import com.freelance.gym.member.validation.MemberUpdateValidation;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.payment.validation.PaymentScheduleValidation;
import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.gym.plan.validation.PlanCreateValidation;
import com.freelance.gym.plan.validation.PlanDeleteValidation;
import com.freelance.identityManagement.PhoneNumber;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationTest {

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    private void resetContext() {
        TestContextHelper.tearDown();
        TestContextHelper.setUp();
    }

    // ==========================================
    // PaymentScheduleValidation Tests
    // ==========================================
    @Test
    void testPaymentScheduleValidation_NullOrEmptyList() {
        PaymentScheduleValidation validation = new PaymentScheduleValidation();
        assertDoesNotThrow(() -> validation.validateScheduleList(null));
        assertDoesNotThrow(() -> validation.validateScheduleList(List.of()));
    }

    @Test
    void testPaymentScheduleValidation_NullElement() {
        PaymentScheduleValidation validation = new PaymentScheduleValidation();
        List<PaymentScheduleDTO> list = new ArrayList<>();
        list.add(null);
        assertThrows(ValidationException.class, () -> validation.validateScheduleList(list));
    }

    @Test
    void testPaymentScheduleValidation_InvalidAmounts() {
        PaymentScheduleValidation validation = new PaymentScheduleValidation();

        // Null amount
        assertThrows(ValidationException.class, () -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(null, BigDecimal.ZERO, LocalDate.now(), null, null, null, null)
        )));
        resetContext();

        // Amount <= 0
        assertThrows(ValidationException.class, () -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), null, null, null, null)
        )));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(-100), BigDecimal.ZERO, LocalDate.now(), null, null, null, null)
        )));
        resetContext();

        // PaidAmount < 0
        assertThrows(ValidationException.class, () -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.valueOf(-10), LocalDate.now(), null, null, null, null)
        )));
        resetContext();

        // PaidAmount > Amount
        assertThrows(ValidationException.class, () -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.valueOf(1500), LocalDate.now(), null, null, null, null)
        )));
    }

    @Test
    void testPaymentScheduleValidation_DueDateAndTransactionId() {
        PaymentScheduleValidation validation = new PaymentScheduleValidation();

        // Due date null
        assertThrows(ValidationException.class, () -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.valueOf(500), null, null, null, null, null)
        )));
        resetContext();

        // TransactionId too long (> 100 chars)
        String longTxnId = "A".repeat(101);
        assertThrows(ValidationException.class, () -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.valueOf(500), LocalDate.now(), null, null, longTxnId, null)
        )));
        resetContext();

        // Valid with null paidAmount
        assertDoesNotThrow(() -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), null, LocalDate.now(), null, null, null, null)
        )));
        resetContext();

        // Valid
        assertDoesNotThrow(() -> validation.validateScheduleList(List.of(
                new PaymentScheduleDTO(BigDecimal.valueOf(1000), BigDecimal.valueOf(500), LocalDate.now(), LocalDate.now(), null, "TXN123", "Notes")
        )));
    }

    // ==========================================
    // PlanCreateValidation Tests
    // ==========================================
    @Test
    void testPlanCreateValidation_NullRequest() {
        PlanCreateValidation validation = new PlanCreateValidation();
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
    }

    @Test
    void testPlanCreateValidation_NameErrors() {
        PlanCreateValidation validation = new PlanCreateValidation();

        // Null name
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new PlanCreateRequestDTO(null, "desc", BigDecimal.valueOf(100), 30)
        ));
        resetContext();

        // Blank name
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new PlanCreateRequestDTO("   ", "desc", BigDecimal.valueOf(100), 30)
        ));
        resetContext();

        // Name > 100 chars
        String longName = "P".repeat(101);
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new PlanCreateRequestDTO(longName, "desc", BigDecimal.valueOf(100), 30)
        ));
    }

    @Test
    void testPlanCreateValidation_PriceAndDurationErrors() {
        PlanCreateValidation validation = new PlanCreateValidation();

        // Negative defaultPrice
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new PlanCreateRequestDTO("Gold", "desc", BigDecimal.valueOf(-1), 30)
        ));
        resetContext();

        // defaultDuration <= 0
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new PlanCreateRequestDTO("Gold", "desc", BigDecimal.valueOf(100), 0)
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new PlanCreateRequestDTO("Gold", "desc", BigDecimal.valueOf(100), -5)
        ));
        resetContext();

        // Valid with null price & duration
        assertDoesNotThrow(() -> validation.validateRequest(
                new PlanCreateRequestDTO("Gold", "desc", null, null)
        ));
        resetContext();

        // Valid with values
        assertDoesNotThrow(() -> validation.validateRequest(
                new PlanCreateRequestDTO("Gold", "desc", BigDecimal.valueOf(500), 60)
        ));
    }

    // ==========================================
    // PlanDeleteValidation Tests
    // ==========================================
    @Test
    void testPlanDeleteValidation() {
        PlanDeleteValidation validation = new PlanDeleteValidation();

        // Null request
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        resetContext();

        // Null planId
        assertThrows(ValidationException.class, () -> validation.validateRequest(new PlanDeleteRequestDTO(null)));
        resetContext();

        // Valid
        assertDoesNotThrow(() -> validation.validateRequest(new PlanDeleteRequestDTO(UUID.randomUUID())));
    }

    // ==========================================
    // MemberCreateValidation Tests
    // ==========================================
    @Test
    void testMemberCreateValidation_NullRequest() {
        MemberCreateValidation validation = new MemberCreateValidation();
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
    }

    @Test
    void testMemberCreateValidation_FieldErrors() {
        MemberCreateValidation validation = new MemberCreateValidation();

        // Null / blank firstName
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO(null, "Doe", new PhoneNumber("91", "9876543210"), List.of(), List.of())
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO("  ", "Doe", new PhoneNumber("91", "9876543210"), List.of(), List.of())
        ));
        resetContext();

        // Null / blank lastName
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO("John", null, new PhoneNumber("91", "9876543210"), List.of(), List.of())
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO("John", "  ", new PhoneNumber("91", "9876543210"), List.of(), List.of())
        ));
        resetContext();

        // Null phoneNumber
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO("John", "Doe", null, List.of(), List.of())
        ));
    }

    @Test
    void testMemberCreateValidation_PlansAndPayments() {
        MemberCreateValidation validation = new MemberCreateValidation();

        // Plans startDate after endDate
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO(
                        "John", "Doe", new PhoneNumber("91", "9876543210"),
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now().plusDays(10), LocalDate.now())),
                        List.of()
                )
        ));
        resetContext();

        // Plans with null element or null start/end date
        List<MemberPlanAssignmentDTO> plansWithNulls = new ArrayList<>();
        plansWithNulls.add(null);
        plansWithNulls.add(new MemberPlanAssignmentDTO(UUID.randomUUID(), null, null));
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberCreateRequestDTO("John", "Doe", new PhoneNumber("91", "9876543210"), plansWithNulls, null)
        ));
        resetContext();

        // Payments invalid amount
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO(
                        "John", "Doe", new PhoneNumber("91", "9876543210"),
                        List.of(),
                        List.of(new PaymentScheduleDTO(null, null, LocalDate.now(), null, null, null, null))
                )
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO(
                        "John", "Doe", new PhoneNumber("91", "9876543210"),
                        List.of(),
                        List.of(new PaymentScheduleDTO(BigDecimal.ZERO, null, LocalDate.now(), null, null, null, null))
                )
        ));
        resetContext();

        // Payments missing dueDate
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberCreateRequestDTO(
                        "John", "Doe", new PhoneNumber("91", "9876543210"),
                        List.of(),
                        List.of(new PaymentScheduleDTO(BigDecimal.valueOf(100), null, null, null, null, null, null))
                )
        ));
        resetContext();

        // Payments with null element
        List<PaymentScheduleDTO> paymentsWithNull = new ArrayList<>();
        paymentsWithNull.add(null);
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberCreateRequestDTO("John", "Doe", new PhoneNumber("91", "9876543210"), List.of(), paymentsWithNull)
        ));
        resetContext();

        // Fully valid
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberCreateRequestDTO(
                        "John", "Doe", new PhoneNumber("91", "9876543210"),
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(1))),
                        List.of(new PaymentScheduleDTO(BigDecimal.valueOf(500), BigDecimal.ZERO, LocalDate.now(), null, null, null, null))
                )
        ));
    }

    // ==========================================
    // MemberRenewValidation Tests
    // ==========================================
    @Test
    void testMemberRenewValidation_Errors() {
        MemberRenewValidation validation = new MemberRenewValidation();

        // Null request
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        resetContext();

        // Null userId
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberRenewRequestDTO(null, List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30))), List.of())
        ));
        resetContext();

        // Null / empty plans
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberRenewRequestDTO(UUID.randomUUID(), null, List.of())
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberRenewRequestDTO(UUID.randomUUID(), List.of(), List.of())
        ));
        resetContext();

        // Plans startDate after endDate
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberRenewRequestDTO(
                        UUID.randomUUID(),
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now().plusDays(10), LocalDate.now())),
                        List.of()
                )
        ));
        resetContext();

        // Payments invalid amount
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberRenewRequestDTO(
                        UUID.randomUUID(),
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30))),
                        List.of(new PaymentScheduleDTO(BigDecimal.valueOf(-1), null, LocalDate.now(), null, null, null, null))
                )
        ));
        resetContext();

        // Payments missing dueDate
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberRenewRequestDTO(
                        UUID.randomUUID(),
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30))),
                        List.of(new PaymentScheduleDTO(BigDecimal.valueOf(100), null, null, null, null, null, null))
                )
        ));
        resetContext();

        // Plans with startDate before endDate / equal
        List<MemberPlanAssignmentDTO> validPlans = List.of(
                new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(10)),
                new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now()),
                new MemberPlanAssignmentDTO(UUID.randomUUID(), null, LocalDate.now())
        );
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberRenewRequestDTO(UUID.randomUUID(), validPlans, null)
        ));
        resetContext();

        // Valid
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberRenewRequestDTO(
                        UUID.randomUUID(),
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(30))),
                        List.of(new PaymentScheduleDTO(BigDecimal.valueOf(500), BigDecimal.ZERO, LocalDate.now(), null, null, null, null))
                )
        ));
    }

    // ==========================================
    // MemberUpdateValidation Tests
    // ==========================================
    @Test
    void testMemberUpdateValidation_Errors() {
        MemberUpdateValidation validation = new MemberUpdateValidation();

        // Null request
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        resetContext();

        // Null userId
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberUpdateRequestDTO(null, null, null, null)
        ));
        resetContext();

        // Plans startDate after endDate
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberUpdateRequestDTO(
                        UUID.randomUUID(),
                        null,
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now().plusDays(10), LocalDate.now())),
                        null
                )
        ));
        resetContext();

        // Plans startDate before/equal or null dates
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberUpdateRequestDTO(
                        UUID.randomUUID(),
                        null,
                        List.of(
                                new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(10)),
                                new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now()),
                                new MemberPlanAssignmentDTO(UUID.randomUUID(), null, null)
                        ),
                        null
                )
        ));
        resetContext();

        // Payments invalid
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberUpdateRequestDTO(
                        UUID.randomUUID(),
                        null,
                        null,
                        List.of(new PaymentScheduleDTO(BigDecimal.ZERO, null, LocalDate.now(), null, null, null, null))
                )
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new MemberUpdateRequestDTO(
                        UUID.randomUUID(),
                        null,
                        null,
                        List.of(new PaymentScheduleDTO(BigDecimal.valueOf(100), null, null, null, null, null, null))
                )
        ));
        resetContext();

        // Payments with null item
        List<PaymentScheduleDTO> paymentsWithNull = new ArrayList<>();
        paymentsWithNull.add(null);
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberUpdateRequestDTO(UUID.randomUUID(), null, null, paymentsWithNull)
        ));
        resetContext();

        // Plans with null item
        List<MemberPlanAssignmentDTO> plansWithNull = new ArrayList<>();
        plansWithNull.add(null);
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberUpdateRequestDTO(UUID.randomUUID(), null, plansWithNull, null)
        ));
        resetContext();

        // Valid update request
        assertDoesNotThrow(() -> validation.validateRequest(
                new MemberUpdateRequestDTO(
                        UUID.randomUUID(),
                        new MemberProfileDTO("John", "Doe", new PhoneNumber("91", "9876543210")),
                        List.of(new MemberPlanAssignmentDTO(UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusMonths(1))),
                        List.of(new PaymentScheduleDTO(BigDecimal.valueOf(500), BigDecimal.ZERO, LocalDate.now(), null, null, null, null))
                )
        ));
    }

    // ==========================================
    // PaymentCollectValidation Tests
    // ==========================================
    @Test
    void testPaymentCollectValidation() {
        com.freelance.gym.payment.validation.PaymentCollectValidation validation =
                new com.freelance.gym.payment.validation.PaymentCollectValidation();

        // Null request
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        resetContext();

        // Null paymentId
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        null,
                        BigDecimal.valueOf(100),
                        LocalDate.now(),
                        com.freelance.gym.enums.PaymentMethod.UPI,
                        "TXN1",
                        "Note"
                )
        ));
        resetContext();

        // Null or non-positive paidAmount
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        UUID.randomUUID(),
                        null,
                        LocalDate.now(),
                        com.freelance.gym.enums.PaymentMethod.UPI,
                        "TXN1",
                        "Note"
                )
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        LocalDate.now(),
                        com.freelance.gym.enums.PaymentMethod.UPI,
                        "TXN1",
                        "Note"
                )
        ));
        resetContext();

        // Null paidDate
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        UUID.randomUUID(),
                        BigDecimal.valueOf(100),
                        null,
                        com.freelance.gym.enums.PaymentMethod.UPI,
                        "TXN1",
                        "Note"
                )
        ));
        resetContext();

        // Null or NONE paymentMethod
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        UUID.randomUUID(),
                        BigDecimal.valueOf(100),
                        LocalDate.now(),
                        null,
                        "TXN1",
                        "Note"
                )
        ));
        resetContext();

        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        UUID.randomUUID(),
                        BigDecimal.valueOf(100),
                        LocalDate.now(),
                        com.freelance.gym.enums.PaymentMethod.NONE,
                        "TXN1",
                        "Note"
                )
        ));
        resetContext();

        // TransactionId too long
        String longTxn = "T".repeat(101);
        assertThrows(ValidationException.class, () -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        UUID.randomUUID(),
                        BigDecimal.valueOf(100),
                        LocalDate.now(),
                        com.freelance.gym.enums.PaymentMethod.UPI,
                        longTxn,
                        "Note"
                )
        ));
        resetContext();

        // Valid request
        assertDoesNotThrow(() -> validation.validateRequest(
                new com.freelance.gym.payment.dto.PaymentCollectRequestDTO(
                        UUID.randomUUID(),
                        BigDecimal.valueOf(100),
                        LocalDate.now(),
                        com.freelance.gym.enums.PaymentMethod.UPI,
                        "TXN1",
                        "Note"
                )
        ));
    }
}
