package com.freelance.gym.payment;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertsEngine;
import com.freelance.alerts.dto.MediaUploadRequestDTO;
import com.freelance.alerts.dto.MediaUploadResponseDTO;
import com.freelance.alerts.dto.UserContactDTO;
import com.freelance.alerts.media.AlertMediaService;
import com.freelance.alerts.preference.service.UserContactService;
import com.freelance.alerts.templates.PaymentReceiptInstantTemplate;
import com.freelance.configuration.system.repository.SystemConfigurationRepository;
import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.fixtures.TestContextHelper;
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.user.UserDTO;
import com.freelance.user.read.UserReadService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceiptNotificationServiceTest {

    @Mock
    private SystemConfigurationRepository systemConfigRepo;

    @Mock
    private UserReadService userReadService;

    @Mock
    private UserContactService userContactService;

    @Mock
    private UserPlanRepository userPlanRepo;

    @Mock
    private PlanRepository planRepo;

    @Mock
    private PaymentRepository paymentRepo;

    @Mock
    private UserPlanPaymentRepository userPlanPaymentRepo;

    @Mock
    private AlertMediaService mediaService;

    @Mock
    private AlertsEngine alertsEngine;

    private ReceiptNotificationService receiptNotificationService;

    @BeforeEach
    void setUp() {
        TestContextHelper.setUp();
        receiptNotificationService = new ReceiptNotificationService(
                systemConfigRepo,
                userReadService,
                userContactService,
                userPlanRepo,
                planRepo,
                paymentRepo,
                userPlanPaymentRepo,
                mediaService,
                alertsEngine
        );
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new ReceiptNotificationService());
    }

    @Test
    void testGenerateAndDispatchReceipt_GuardAgainstInvalidParams() {
        receiptNotificationService.generateAndDispatchReceipt(null, UUID.randomUUID(), BigDecimal.valueOf(100), "UPI", null, LocalDate.now(), List.of());
        receiptNotificationService.generateAndDispatchReceipt(UUID.randomUUID(), UUID.randomUUID(), null, "UPI", null, LocalDate.now(), List.of());
        receiptNotificationService.generateAndDispatchReceipt(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "UPI", null, LocalDate.now(), List.of());
        verifyNoInteractions(mediaService);
        verifyNoInteractions(alertsEngine);
    }

    @Test
    void testGenerateAndDispatchReceipt_FullFlowWithSiblingInstallments() {
        UUID userId = UUID.randomUUID();
        UUID userPlanId1 = UUID.randomUUID();
        UUID planId1 = UUID.randomUUID();
        UUID paymentId1 = UUID.randomUUID();
        UUID paymentId2 = UUID.randomUUID();

        // 1. Mock Gym Configs & User
        when(systemConfigRepo.getAll()).thenReturn(Map.of("gym.profile.name", "Gold Gym"));
        when(userReadService.readUsersByIds(List.of(userId))).thenReturn(List.of(
                new UserDTO(userId, "mem_9876543210", "pwd", "Rahul", "Sharma", LocalDateTime.now(), LocalDateTime.now())
        ));
        when(userContactService.findByUserAndChannel(userId, AlertChannel.WHATSAPP)).thenReturn(Optional.of(
                new UserContactDTO(UUID.randomUUID(), userId, AlertChannel.WHATSAPP, "+919876543210", false, true)
        ));

        // 2. Mock Plan & Sibling Mappings
        when(userPlanPaymentRepo.findByPaymentId(paymentId1)).thenReturn(List.of(
                new UserPlanPaymentEntity(userPlanId1, paymentId1)
        ));
        when(userPlanPaymentRepo.findByUserPlanIds(List.of(userPlanId1))).thenReturn(List.of(
                new UserPlanPaymentEntity(userPlanId1, paymentId1),
                new UserPlanPaymentEntity(userPlanId1, paymentId2)
        ));

        PlanEntity planEntity = new PlanEntity(planId1, "1 Month Gold", "Access", BigDecimal.valueOf(6000), 30, true);
        when(planRepo.get(planId1)).thenReturn(planEntity);

        List<UserPlanResponseDTO> coveredPlans = List.of(
                new UserPlanResponseDTO(userPlanId1, userId, planId1, "1 Month Gold", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        );

        // 3. Mock Sibling Payment Entities
        PaymentEntity p1 = new PaymentEntity(paymentId1, BigDecimal.valueOf(3000), BigDecimal.valueOf(3000), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), PaymentMethod.UPI, "TXN_UPI_001", "Upfront");
        PaymentEntity p2 = new PaymentEntity(paymentId2, BigDecimal.valueOf(3000), BigDecimal.ZERO, LocalDate.of(2026, 2, 1), null, PaymentMethod.NONE, null, "Due next month");
        when(paymentRepo.findByIds(anyList())).thenReturn(List.of(p1, p2));

        // 4. Mock Media Service Upload
        when(mediaService.uploadMedia(any(MediaUploadRequestDTO.class))).thenReturn(
                new MediaUploadResponseDTO("media-rec-001", "https://media.freelance.com/receipt.pdf", "Receipt_TXN_UPI_001.pdf", "application/pdf", 1024L)
        );

        // Execute
        receiptNotificationService.generateAndDispatchReceipt(
                userId,
                paymentId1,
                BigDecimal.valueOf(3000),
                "UPI",
                "TXN_UPI_001",
                LocalDate.of(2026, 1, 1),
                coveredPlans
        );

        // Verify media upload occurred with PDF
        ArgumentCaptor<MediaUploadRequestDTO> uploadCaptor = ArgumentCaptor.forClass(MediaUploadRequestDTO.class);
        verify(mediaService).uploadMedia(uploadCaptor.capture());
        assertEquals("application/pdf", uploadCaptor.getValue().mimeType());
        assertEquals("Payment_Receipt_Rahul_" + LocalDate.now() + ".pdf", uploadCaptor.getValue().fileName());

        // Verify alert emitted with mediaUrl and mediaId
        verify(alertsEngine).emitAlert(
                any(PaymentReceiptInstantTemplate.class),
                eq(userId),
                eq("https://media.freelance.com/receipt.pdf"),
                eq("media-rec-001"),
                eq("DOCUMENT")
        );
    }
}
