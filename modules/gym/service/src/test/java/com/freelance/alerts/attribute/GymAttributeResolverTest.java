package com.freelance.alerts.attribute;

import com.freelance.configuration.system.repository.SystemConfigurationRepository;
import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.keys.branding.BrandConfigKeys;
import com.freelance.keys.gym.GymProfileConfigKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GymAttributeResolver Real Production Data Resolution Tests")
class GymAttributeResolverTest {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Mock
    private SystemConfigurationRepository systemConfigRepo;
    @Mock
    private PlanRepository planRepo;
    @Mock
    private UserPlanRepository userPlanRepo;
    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private UserPlanPaymentRepository userPlanPaymentRepo;

    private GymAttributeResolver resolver;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        resolver = new GymAttributeResolver(
                systemConfigRepo,
                planRepo,
                userPlanRepo,
                paymentRepo,
                userPlanPaymentRepo
        );
        testUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Verify resolver metadata and supported attributes")
    void testMetadata() {
        assertEquals("GYM", resolver.getDomain());
        Set<String> supported = resolver.getSupportedAttributes();
        assertTrue(supported.contains("GYM.name"));
        assertTrue(supported.contains("GYM.timings"));
        assertTrue(supported.contains("GYM.support_phone"));
        assertTrue(supported.contains("GYM.address"));
        assertTrue(supported.contains("GYM.google_maps_url"));
        assertTrue(supported.contains("GYM.whatsapp_channel_url"));
        assertTrue(supported.contains("GYM.payment_qr"));
        assertTrue(supported.contains("GYM.plan_catalogue"));
        assertTrue(supported.contains("PLAN.name"));
        assertTrue(supported.contains("PLAN.expiry_date"));
        assertTrue(supported.contains("PLAN.days_left"));
        assertTrue(supported.contains("MEMBER.plans_breakdown"));
        assertTrue(supported.contains("PAYMENT.amount"));
        assertTrue(supported.contains("PAYMENT.due_date"));
        assertTrue(supported.contains("PAYMENT.days_left"));
        assertTrue(supported.contains("PAYMENT.receipt_id"));
        assertTrue(supported.contains("PAYMENT.total_paid"));
        assertTrue(supported.contains("PAYMENT.pending_balance"));
        assertTrue(supported.contains("PAYMENT.next_due_summary"));
    }

    @Test
    @DisplayName("Verify real gym brand and profile attributes resolution from SystemConfigurationRepository")
    void testResolveGymBrandAndProfile() {
        Map<String, String> configs = Map.of(
                BrandConfigKeys.GYM_NAME.getKey(), "Olympus Fitness Center",
                GymProfileConfigKeys.GYM_TIMINGS.getKey(), "5:30 AM - 10:30 PM (Mon-Sat), 7:00 AM - 1:00 PM (Sun)",
                GymProfileConfigKeys.GYM_SUPPORT_PHONE.getKey(), "+919876543210",
                GymProfileConfigKeys.GYM_ADDRESS.getKey(), "42 Olympus Peak, Koregaon Park, Pune",
                GymProfileConfigKeys.GYM_GOOGLE_MAPS_URL.getKey(), "https://maps.google.com/?q=Olympus+Fitness+Pune",
                GymProfileConfigKeys.GYM_WHATSAPP_CHANNEL_URL.getKey(), "https://whatsapp.com/channel/olympusfit",
                GymProfileConfigKeys.GYM_UPI_QR_MEDIA_ID.getKey(), "https://example.com/upi-qr.png"
        );
        when(systemConfigRepo.getAll()).thenReturn(configs);

        Set<String> requested = Set.of(
                "GYM.name",
                "GYM.timings",
                "GYM.support_phone",
                "GYM.address",
                "GYM.google_maps_url",
                "GYM.whatsapp_channel_url",
                "GYM.payment_qr"
        );

        Map<String, String> resolved = resolver.resolve(testUserId, requested);

        assertEquals("Olympus Fitness Center", resolved.get("GYM.name"));
        assertEquals("5:30 AM - 10:30 PM (Mon-Sat), 7:00 AM - 1:00 PM (Sun)", resolved.get("GYM.timings"));
        assertEquals("+919876543210", resolved.get("GYM.support_phone"));
        assertEquals("42 Olympus Peak, Koregaon Park, Pune", resolved.get("GYM.address"));
        assertEquals("https://maps.google.com/?q=Olympus+Fitness+Pune", resolved.get("GYM.google_maps_url"));
        assertEquals("https://whatsapp.com/channel/olympusfit", resolved.get("GYM.whatsapp_channel_url"));
        assertEquals("https://example.com/upi-qr.png", resolved.get("GYM.payment_qr"));
    }

    @Test
    @DisplayName("Verify real plan catalogue resolution from PlanRepository")
    void testResolvePlanCatalogue() {
        PlanEntity plan1 = new PlanEntity(UUID.randomUUID(), "Monthly Membership", "Single month full access", BigDecimal.valueOf(2500), 30, true);
        PlanEntity plan2 = new PlanEntity(UUID.randomUUID(), "Quarterly Transformation", "3 months unlimited access", BigDecimal.valueOf(6500), 90, true);
        PlanEntity plan3 = new PlanEntity(UUID.randomUUID(), "Annual Gold VIP", "12 months VIP access", BigDecimal.valueOf(18000), 365, true);

        when(systemConfigRepo.getAll()).thenReturn(Collections.emptyMap());
        when(planRepo.findAllActivePlans()).thenReturn(List.of(plan1, plan2, plan3));

        Map<String, String> resolved = resolver.resolve(testUserId, Set.of("GYM.plan_catalogue"));

        String catalogue = resolved.get("GYM.plan_catalogue");
        assertNotNull(catalogue);
        assertTrue(catalogue.contains("• Monthly Membership: ₹2,500 (30 Days)"));
        assertTrue(catalogue.contains("• Quarterly Transformation: ₹6,500 (90 Days)"));
        assertTrue(catalogue.contains("• Annual Gold VIP: ₹18,000 (365 Days)"));
    }

    @Test
    @DisplayName("Verify real member plans breakdown and expiry calculation from UserPlanRepository")
    void testResolveMemberPlansBreakdown() {
        UUID planId = UUID.randomUUID();
        PlanEntity planEntity = new PlanEntity(planId, "Annual Gold VIP", "Full access", BigDecimal.valueOf(18000), 365, true);

        LocalDate startDate = LocalDate.now().minusDays(15);
        LocalDate endDate = LocalDate.now().plusDays(350);

        UserPlanEntity userPlan = new UserPlanEntity(testUserId, planId, startDate, endDate);
        userPlan.setId(UUID.randomUUID());

        when(systemConfigRepo.getAll()).thenReturn(Collections.emptyMap());
        when(userPlanRepo.findByUserId(testUserId)).thenReturn(List.of(userPlan));
        when(planRepo.findByIds(List.of(planId))).thenReturn(List.of(planEntity));

        Set<String> requested = Set.of(
                "MEMBER.plans_breakdown",
                "PLAN.name",
                "PLAN.expiry_date",
                "PLAN.days_left"
        );

        Map<String, String> resolved = resolver.resolve(testUserId, requested);

        assertEquals("Annual Gold VIP", resolved.get("PLAN.name"));
        assertEquals(endDate.format(DATE_FMT), resolved.get("PLAN.expiry_date"));
        assertEquals("350", resolved.get("PLAN.days_left"));

        String breakdown = resolved.get("MEMBER.plans_breakdown");
        assertNotNull(breakdown);
        assertTrue(breakdown.contains("• Annual Gold VIP"));
        assertTrue(breakdown.contains(startDate.format(DATE_FMT) + " – " + endDate.format(DATE_FMT)));
        assertTrue(breakdown.contains("(350 days left)"));
    }

    @Test
    @DisplayName("Verify that the plan about to expire soonest is selected for renewal reminder")
    void testResolveExpiringPlanSelection() {
        UUID plan1Id = UUID.randomUUID();
        PlanEntity multiYearPlan = new PlanEntity(plan1Id, "3-Year Multi Pass", "Long term pass", BigDecimal.valueOf(30000), 1095, true);

        UUID plan2Id = UUID.randomUUID();
        PlanEntity expiringPlan = new PlanEntity(plan2Id, "Monthly Fitness", "Expiring soon", BigDecimal.valueOf(2000), 30, true);

        UUID plan3Id = UUID.randomUUID();
        PlanEntity expiredPlan = new PlanEntity(plan3Id, "Trial Pass", "Expired", BigDecimal.valueOf(500), 7, true);

        // Multi-year pass ends in 1000 days
        UserPlanEntity upMultiYear = new UserPlanEntity(testUserId, plan1Id, LocalDate.now().minusDays(95), LocalDate.now().plusDays(1000));
        upMultiYear.setId(UUID.randomUUID());

        // Monthly fitness ends in 4 days (about to expire!)
        LocalDate soonExpDate = LocalDate.now().plusDays(4);
        UserPlanEntity upExpiring = new UserPlanEntity(testUserId, plan2Id, LocalDate.now().minusDays(26), soonExpDate);
        upExpiring.setId(UUID.randomUUID());

        // Trial pass expired 20 days ago
        UserPlanEntity upExpired = new UserPlanEntity(testUserId, plan3Id, LocalDate.now().minusDays(27), LocalDate.now().minusDays(20));
        upExpired.setId(UUID.randomUUID());

        // Simulated repo returns user plans (often ordered by startDate DESC)
        when(systemConfigRepo.getAll()).thenReturn(Collections.emptyMap());
        when(userPlanRepo.findByUserId(testUserId)).thenReturn(List.of(upMultiYear, upExpiring, upExpired));
        when(planRepo.findByIds(anyList())).thenReturn(List.of(multiYearPlan, expiringPlan, expiredPlan));

        Set<String> requested = Set.of("PLAN.name", "PLAN.expiry_date", "PLAN.days_left");
        Map<String, String> resolved = resolver.resolve(testUserId, requested);

        // Must pick the one about to expire (Monthly Fitness), NOT the multi-year pass!
        assertEquals("Monthly Fitness", resolved.get("PLAN.name"));
        assertEquals(soonExpDate.format(DATE_FMT), resolved.get("PLAN.expiry_date"));
        assertEquals("4", resolved.get("PLAN.days_left"));
    }

    @Test
    @DisplayName("Verify that multiple plans expiring within the renewal cycle (e.g. Gym & PT) are all listed in PLAN.name")
    void testResolveMultipleExpiringPlansSelection() {
        UUID gymPlanId = UUID.randomUUID();
        PlanEntity gymPlan = new PlanEntity(gymPlanId, "Gym", "Full gym floor access", BigDecimal.valueOf(3000), 30, true);

        UUID ptPlanId = UUID.randomUUID();
        PlanEntity ptPlan = new PlanEntity(ptPlanId, "Personal Training", "1-on-1 PT sessions", BigDecimal.valueOf(3000), 30, true);

        UUID futurePlanId = UUID.randomUUID();
        PlanEntity futurePlan = new PlanEntity(futurePlanId, "Annual VIP", "Long term VIP", BigDecimal.valueOf(25000), 365, true);

        // Both Gym and PT end on the exact same date (e.g. 20 days from now, or end of month)
        LocalDate expiryDate = LocalDate.now().plusDays(20);
        UserPlanEntity upGym = new UserPlanEntity(testUserId, gymPlanId, LocalDate.now().minusDays(10), expiryDate);
        upGym.setId(UUID.randomUUID());

        UserPlanEntity upPt = new UserPlanEntity(testUserId, ptPlanId, LocalDate.now().minusDays(10), expiryDate);
        upPt.setId(UUID.randomUUID());

        UserPlanEntity upFuture = new UserPlanEntity(testUserId, futurePlanId, LocalDate.now().minusDays(10), LocalDate.now().plusDays(300));
        upFuture.setId(UUID.randomUUID());

        when(systemConfigRepo.getAll()).thenReturn(Collections.emptyMap());
        when(userPlanRepo.findByUserId(testUserId)).thenReturn(List.of(upGym, upPt, upFuture));
        when(planRepo.findByIds(anyList())).thenReturn(List.of(gymPlan, ptPlan, futurePlan));

        Set<String> requested = Set.of("PLAN.name", "PLAN.expiry_date", "PLAN.days_left");
        Map<String, String> resolved = resolver.resolve(testUserId, requested);

        // Both Gym and Personal Training must be included!
        String resolvedNames = resolved.get("PLAN.name");
        assertNotNull(resolvedNames);
        assertTrue(resolvedNames.contains("Gym"), "Must contain Gym");
        assertTrue(resolvedNames.contains("Personal Training"), "Must contain Personal Training");
        assertFalse(resolvedNames.contains("Annual VIP"), "Must NOT contain future Annual VIP");
        assertEquals(expiryDate.format(DATE_FMT), resolved.get("PLAN.expiry_date"));
        assertEquals("20", resolved.get("PLAN.days_left"));
    }

    @Test
    @DisplayName("Verify real member payments and ledger resolution from PaymentRepository")
    void testResolvePaymentLedger() {
        UUID upId = UUID.randomUUID();
        UserPlanEntity userPlan = new UserPlanEntity(testUserId, UUID.randomUUID(), LocalDate.now().minusDays(10), LocalDate.now().plusDays(80));
        userPlan.setId(upId);

        UUID pay1Id = UUID.randomUUID();
        PaymentEntity paidPart = new PaymentEntity(
                pay1Id,
                BigDecimal.valueOf(12000),
                BigDecimal.valueOf(12000),
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(10),
                PaymentMethod.UPI,
                "TXN_UPI_987654",
                "Initial installment"
        );

        UUID pay2Id = UUID.randomUUID();
        LocalDate nextDueDate = LocalDate.now().plusDays(20);
        PaymentEntity duePart = new PaymentEntity(
                pay2Id,
                BigDecimal.valueOf(6000),
                BigDecimal.ZERO,
                nextDueDate,
                null,
                PaymentMethod.NONE,
                null,
                "Pending balance"
        );

        UserPlanPaymentEntity mapping1 = new UserPlanPaymentEntity(upId, pay1Id);
        UserPlanPaymentEntity mapping2 = new UserPlanPaymentEntity(upId, pay2Id);

        when(systemConfigRepo.getAll()).thenReturn(Collections.emptyMap());
        when(userPlanRepo.findByUserId(testUserId)).thenReturn(List.of(userPlan));
        when(userPlanPaymentRepo.findByUserPlanIds(List.of(upId))).thenReturn(List.of(mapping1, mapping2));
        when(paymentRepo.findByIds(List.of(pay1Id, pay2Id))).thenReturn(List.of(paidPart, duePart));

        Set<String> requested = Set.of(
                "PAYMENT.total_paid",
                "PAYMENT.pending_balance",
                "PAYMENT.next_due_summary",
                "PAYMENT.due_date",
                "PAYMENT.amount",
                "PAYMENT.receipt_id"
        );

        Map<String, String> resolved = resolver.resolve(testUserId, requested);

        assertEquals("₹12,000", resolved.get("PAYMENT.total_paid"));
        assertEquals("₹6,000", resolved.get("PAYMENT.pending_balance"));
        assertEquals("₹6,000 (Due: " + nextDueDate.format(DATE_FMT) + ")", resolved.get("PAYMENT.next_due_summary"));
        assertEquals(nextDueDate.format(DATE_FMT), resolved.get("PAYMENT.due_date"));
        assertEquals("₹12,000", resolved.get("PAYMENT.amount"));
        assertEquals("TXN_UPI_987654", resolved.get("PAYMENT.receipt_id"));
    }

    @Test
    @DisplayName("Verify upcoming payment reminder resolves due amount, days left, and specific payment plan")
    void testResolveUpcomingPaymentReminder() {
        UUID planId = UUID.randomUUID();
        PlanEntity gymPlan = new PlanEntity(planId, "Strength Gym", "Gym plan", BigDecimal.valueOf(10000), 90, true);

        UUID upId = UUID.randomUUID();
        UserPlanEntity userPlan = new UserPlanEntity(testUserId, planId, LocalDate.now().minusDays(10), LocalDate.now().plusDays(80));
        userPlan.setId(upId);

        UUID payId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.now().plusDays(7);
        PaymentEntity upcomingPayment = new PaymentEntity(
                payId,
                BigDecimal.valueOf(5000),
                BigDecimal.ZERO,
                dueDate,
                null,
                PaymentMethod.NONE,
                null,
                "Second installment"
        );

        UserPlanPaymentEntity mapping = new UserPlanPaymentEntity(upId, payId);

        when(systemConfigRepo.getAll()).thenReturn(Collections.emptyMap());
        when(userPlanRepo.findByUserId(testUserId)).thenReturn(List.of(userPlan));
        when(userPlanPaymentRepo.findByUserPlanIds(List.of(upId))).thenReturn(List.of(mapping));
        when(paymentRepo.findByIds(List.of(payId))).thenReturn(List.of(upcomingPayment));
        when(planRepo.findByIds(List.of(planId))).thenReturn(List.of(gymPlan));

        Set<String> requested = Set.of(
                "PAYMENT.amount",
                "PAYMENT.due_date",
                "PAYMENT.days_left",
                "PLAN.name"
        );

        Map<String, String> resolved = resolver.resolve(testUserId, requested);

        assertEquals("₹5,000", resolved.get("PAYMENT.amount"));
        assertEquals(dueDate.format(DATE_FMT), resolved.get("PAYMENT.due_date"));
        assertEquals("7", resolved.get("PAYMENT.days_left"));
        assertEquals("Strength Gym", resolved.get("PLAN.name"));
    }

    @Test
    @DisplayName("Verify overdue payment reminder resolves itemized overdue breakdown, overdue balance, and earliest overdue date")
    void testResolvePaymentOverdueReminder() {
        UUID planId = UUID.randomUUID();
        PlanEntity gymPlan = new PlanEntity(planId, "Strength Gym", "Gym plan", BigDecimal.valueOf(10000), 90, true);

        UUID upId = UUID.randomUUID();
        UserPlanEntity userPlan = new UserPlanEntity(testUserId, planId, LocalDate.now().minusDays(60), LocalDate.now().plusDays(30));
        userPlan.setId(upId);

        UUID payId1 = UUID.randomUUID();
        LocalDate due1 = LocalDate.now().minusDays(30);
        PaymentEntity overdue1 = new PaymentEntity(
                payId1,
                BigDecimal.valueOf(2000),
                BigDecimal.ZERO,
                due1,
                null,
                PaymentMethod.NONE,
                null,
                "First overdue installment"
        );

        UUID payId2 = UUID.randomUUID();
        LocalDate due2 = LocalDate.now().minusDays(10);
        PaymentEntity overdue2 = new PaymentEntity(
                payId2,
                BigDecimal.valueOf(3000),
                BigDecimal.ZERO,
                due2,
                null,
                PaymentMethod.NONE,
                null,
                "Second overdue installment"
        );

        UserPlanPaymentEntity mapping1 = new UserPlanPaymentEntity(upId, payId1);
        UserPlanPaymentEntity mapping2 = new UserPlanPaymentEntity(upId, payId2);

        when(systemConfigRepo.getAll()).thenReturn(Collections.emptyMap());
        when(userPlanRepo.findByUserId(testUserId)).thenReturn(List.of(userPlan));
        when(userPlanPaymentRepo.findByUserPlanIds(List.of(upId))).thenReturn(List.of(mapping1, mapping2));
        when(paymentRepo.findByIds(List.of(payId1, payId2))).thenReturn(List.of(overdue2, overdue1)); // intentionally unordered to verify sort
        when(planRepo.findByIds(List.of(planId))).thenReturn(List.of(gymPlan));

        Set<String> requested = Set.of(
                "PAYMENT.overdue_breakdown",
                "PAYMENT.pending_balance",
                "PAYMENT.overdue_since"
        );

        Map<String, String> resolved = resolver.resolve(testUserId, requested);

        assertEquals("₹5,000", resolved.get("PAYMENT.pending_balance"));
        assertEquals(due1.format(DATE_FMT), resolved.get("PAYMENT.overdue_since"));
        String breakdown = resolved.get("PAYMENT.overdue_breakdown");
        assertNotNull(breakdown);
        assertTrue(breakdown.contains("• ₹2,000 (Due: " + due1.format(DATE_FMT) + ")"));
        assertTrue(breakdown.contains("• ₹3,000 (Due: " + due2.format(DATE_FMT) + ")"));
        assertTrue(breakdown.indexOf(due1.format(DATE_FMT)) < breakdown.indexOf(due2.format(DATE_FMT)), "Earliest overdue should be listed first");
    }
}
