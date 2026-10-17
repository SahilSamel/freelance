package com.freelance.gym.assembler;

import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.member.assembler.MemberAssembler;
import com.freelance.gym.member.dto.MemberRecordDTO;
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.assembler.PaymentAssembler;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.plan.assembler.PlanAssembler;
import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.dto.PlanResponseDTO;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.util.PasswordGenerator;
import com.freelance.identityManagement.PhoneNumber;
import com.freelance.user.UserDTO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AssemblerAndUtilTest {

    // ==========================================
    // MemberAssembler Tests
    // ==========================================
    @Test
    void testMemberAssembler_NullUser() {
        MemberAssembler assembler = new MemberAssembler();
        assertNull(assembler.toMemberRecord(null, null, "John", "Doe", null, null));
    }

    @Test
    void testMemberAssembler_WithCustomAndDefaultNames() {
        MemberAssembler assembler = new MemberAssembler();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        UserDTO user = new UserDTO(userId, "john_user", "password123", "UserFirst", "UserLast", now, null);
        PhoneNumber phone = new PhoneNumber("91", "9876543210");
        List<PaymentResponseDTO> payments = List.of(
                new PaymentResponseDTO(UUID.randomUUID(), BigDecimal.valueOf(500), BigDecimal.ZERO, LocalDate.now(), null, PaymentMethod.NONE, null, null, List.of())
        );

        List<UserPlanResponseDTO> plans = List.of(
                new UserPlanResponseDTO(UUID.randomUUID(), userId, UUID.randomUUID(), "VIP", LocalDate.now(), LocalDate.now().plusDays(30))
        );

        // Custom first and last name
        MemberRecordDTO record1 = assembler.toMemberRecord(user, phone, "CustomFirst", "CustomLast", plans, payments);
        assertNotNull(record1);
        assertEquals(userId, record1.userId());
        assertEquals("john_user", record1.username());
        assertEquals("CustomFirst", record1.firstName());
        assertEquals("CustomLast", record1.lastName());
        assertEquals(phone, record1.phoneNumber());
        assertEquals(plans, record1.plans());
        assertEquals(payments, record1.payments());

        // Null first/last name, null plans and null payments -> fallback to user entity and empty list
        MemberRecordDTO record2 = assembler.toMemberRecord(user, phone, null, null, null, null);
        assertNotNull(record2);
        assertEquals("UserFirst", record2.firstName());
        assertEquals("UserLast", record2.lastName());
        assertTrue(record2.plans().isEmpty());
        assertTrue(record2.payments().isEmpty());
    }

    // ==========================================
    // PaymentAssembler Tests
    // ==========================================
    @Test
    void testPaymentAssembler_ToEntity() {
        PaymentAssembler assembler = new PaymentAssembler();
        assertNull(assembler.toEntity(null));

        // With null paidAmount and null paymentMethod
        PaymentScheduleDTO dto1 = new PaymentScheduleDTO(
                BigDecimal.valueOf(1000),
                null,
                LocalDate.of(2026, 10, 1),
                null,
                null,
                "TXN1",
                "Note1"
        );
        PaymentEntity entity1 = assembler.toEntity(dto1);
        assertNotNull(entity1);
        assertNotNull(entity1.getId());
        assertEquals(BigDecimal.valueOf(1000), entity1.getAmount());
        assertEquals(BigDecimal.ZERO, entity1.getPaidAmount());
        assertEquals(PaymentMethod.NONE, entity1.getPaymentMethod());
        assertEquals("TXN1", entity1.getTransactionId());
        assertEquals("Note1", entity1.getNotes());

        // With full fields
        PaymentScheduleDTO dto2 = new PaymentScheduleDTO(
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 1),
                PaymentMethod.UPI,
                "TXN2",
                "Note2"
        );
        PaymentEntity entity2 = assembler.toEntity(dto2);
        assertNotNull(entity2);
        assertEquals(BigDecimal.valueOf(1000), entity2.getPaidAmount());
        assertEquals(PaymentMethod.UPI, entity2.getPaymentMethod());
    }

    @Test
    void testPaymentAssembler_FromEntity() {
        PaymentAssembler assembler = new PaymentAssembler();
        assertNull(assembler.fromEntity(null, null));

        UUID paymentId = UUID.randomUUID();
        PaymentEntity entity = new PaymentEntity(
                paymentId,
                BigDecimal.valueOf(2000),
                BigDecimal.valueOf(1000),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                PaymentMethod.CASH,
                "TXN_CASH",
                "Notes"
        );

        // With null plans list
        PaymentResponseDTO res1 = assembler.fromEntity(entity, null);
        assertNotNull(res1);
        assertEquals(paymentId, res1.id());
        assertEquals(BigDecimal.valueOf(2000), res1.amount());
        assertEquals(BigDecimal.valueOf(1000), res1.paidAmount());
        assertEquals(LocalDate.of(2026, 10, 1), res1.dueDate());
        assertEquals(LocalDate.of(2026, 10, 2), res1.paidDate());
        assertEquals(PaymentMethod.CASH, res1.paymentMethod());
        assertEquals("TXN_CASH", res1.transactionId());
        assertEquals("Notes", res1.notes());
        assertTrue(res1.plans().isEmpty());

        // With plans list
        List<UserPlanResponseDTO> plans = List.of(
                new UserPlanResponseDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Monthly Plan", LocalDate.now(), LocalDate.now().plusMonths(1))
        );
        PaymentResponseDTO res2 = assembler.fromEntity(entity, plans);
        assertNotNull(res2);
        assertEquals(plans, res2.plans());
    }

    // ==========================================
    // PlanAssembler Tests
    // ==========================================
    @Test
    void testPlanAssembler_ToEntity() {
        PlanAssembler assembler = new PlanAssembler();
        assertNull(assembler.toEntity(null));

        // With null price and duration
        PlanCreateRequestDTO req1 = new PlanCreateRequestDTO("  Basic Plan  ", "Basic Desc", null, null);
        PlanEntity entity1 = assembler.toEntity(req1);
        assertNotNull(entity1);
        assertNotNull(entity1.getId());
        assertEquals("Basic Plan", entity1.getName());
        assertEquals("Basic Desc", entity1.getDescription());
        assertEquals(BigDecimal.ZERO, entity1.getDefaultPrice());
        assertEquals(30, entity1.getDefaultDuration());
        assertTrue(entity1.isActive());

        // With custom price and duration
        PlanCreateRequestDTO req2 = new PlanCreateRequestDTO("Annual", "Annual Desc", BigDecimal.valueOf(12000), 365);
        PlanEntity entity2 = assembler.toEntity(req2);
        assertNotNull(entity2);
        assertEquals(BigDecimal.valueOf(12000), entity2.getDefaultPrice());
        assertEquals(365, entity2.getDefaultDuration());
    }

    @Test
    void testPlanAssembler_FromEntity() {
        PlanAssembler assembler = new PlanAssembler();
        assertNull(assembler.fromEntity(null));

        UUID planId = UUID.randomUUID();
        PlanEntity entity = new PlanEntity(planId, "Gold", "Gold Desc", BigDecimal.valueOf(5000), 90, true);
        PlanResponseDTO dto = assembler.fromEntity(entity);
        assertNotNull(dto);
        assertEquals(planId, dto.id());
        assertEquals("Gold", dto.name());
        assertEquals("Gold Desc", dto.description());
        assertEquals(BigDecimal.valueOf(5000), dto.defaultPrice());
        assertEquals(90, dto.defaultDuration());
        assertTrue(dto.isActive());
    }

    // ==========================================
    // PasswordGenerator Tests
    // ==========================================
    @Test
    void testPasswordGenerator() throws Exception {
        // Test private constructor via reflection
        Constructor<PasswordGenerator> constructor = PasswordGenerator.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        PasswordGenerator generator = constructor.newInstance();
        assertNotNull(generator);

        String pwd1 = PasswordGenerator.generateRandomPassword();
        assertNotNull(pwd1);
        assertEquals(10, pwd1.length());

        String pwd2 = PasswordGenerator.generateRandomPassword(16);
        assertNotNull(pwd2);
        assertEquals(16, pwd2.length());

        // Length less than 8 should default to 8
        String pwd3 = PasswordGenerator.generateRandomPassword(4);
        assertNotNull(pwd3);
        assertEquals(8, pwd3.length());
    }
}
