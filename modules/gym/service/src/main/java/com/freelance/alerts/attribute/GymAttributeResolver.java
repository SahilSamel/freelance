package com.freelance.alerts.attribute;

import com.freelance.configuration.system.repository.SystemConfigurationRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves GYM, PLAN, PAYMENT, and MEMBER attributes dynamically for WhatsApp and communication templates.
 */
public class GymAttributeResolver implements IDomainAttributeResolver {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final SystemConfigurationRepository systemConfigRepo;
    private final PlanRepository planRepo;
    private final UserPlanRepository userPlanRepo;
    private final PaymentRepository paymentRepo;
    private final UserPlanPaymentRepository userPlanPaymentRepo;

    public GymAttributeResolver() {
        this(
                new SystemConfigurationRepository(),
                new PlanRepository(),
                new UserPlanRepository(),
                new PaymentRepository(),
                new UserPlanPaymentRepository()
        );
    }

    public GymAttributeResolver(
            SystemConfigurationRepository systemConfigRepo,
            PlanRepository planRepo,
            UserPlanRepository userPlanRepo,
            PaymentRepository paymentRepo,
            UserPlanPaymentRepository userPlanPaymentRepo
    ) {
        this.systemConfigRepo = systemConfigRepo;
        this.planRepo = planRepo;
        this.userPlanRepo = userPlanRepo;
        this.paymentRepo = paymentRepo;
        this.userPlanPaymentRepo = userPlanPaymentRepo;
    }

    private static final Map<String, String> ATTRIBUTE_DESCRIPTIONS = Map.ofEntries(
            Map.entry("GYM.name", "Brand display name of the fitness center"),
            Map.entry("GYM.timings", "Operational hours and timings"),
            Map.entry("GYM.support_phone", "Front desk reception WhatsApp contact phone number"),
            Map.entry("GYM.address", "Physical location of the facility"),
            Map.entry("GYM.google_maps_url", "Google Maps listing / reviews link"),
            Map.entry("GYM.whatsapp_channel_url", "Official Gym WhatsApp Community Channel link"),
            Map.entry("GYM.payment_qr", "Payment QR code URL or UPI link for online membership payment"),
            Map.entry("GYM.plan_catalogue", "Formatted list of all currently active membership plans & pricing"),
            Map.entry("PLAN.name", "Name of the member's most recent or expiring plan"),
            Map.entry("PLAN.expiry_date", "Expiry date of the member's current active/latest plan"),
            Map.entry("PLAN.days_left", "Number of days remaining until plan expiry"),
            Map.entry("MEMBER.plans_breakdown", "Itemized list of all active plans with duration and remaining days"),
            Map.entry("PAYMENT.amount", "Amount of the latest payment or pending installment"),
            Map.entry("PAYMENT.due_date", "Due date of the next pending installment"),
            Map.entry("PAYMENT.receipt_id", "Receipt identifier of the payment transaction"),
            Map.entry("PAYMENT.mode", "Payment method mode used for the transaction"),
            Map.entry("PAYMENT.paid_date", "Date on which the payment was recorded"),
            Map.entry("PAYMENT.total_paid", "Sum of all payments collected from this member"),
            Map.entry("PAYMENT.pending_balance", "Total outstanding pending dues for this member"),
            Map.entry("PAYMENT.next_due_summary", "Summary of next upcoming installment due amount and date"),
            Map.entry("PAYMENT.days_left", "Number of days remaining until next installment due date"),
            Map.entry("PAYMENT.overdue_breakdown", "Itemized list of all overdue installments with amount and due dates"),
            Map.entry("PAYMENT.overdue_since", "Earliest due date among all past overdue installments")
    );

    @Override
    public String getDomain() {
        return "GYM";
    }

    @Override
    public Set<String> getSupportedAttributes() {
        return ATTRIBUTE_DESCRIPTIONS.keySet();
    }

    @Override
    public String getAttributeDescription(String attributeKey) {
        return ATTRIBUTE_DESCRIPTIONS.get(attributeKey);
    }

    @Override
    public Map<String, String> resolve(UUID userId, Set<String> requestedAttributes) {
        if (requestedAttributes == null || requestedAttributes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> resolved = new HashMap<>();
        Map<String, String> sysConfigs = systemConfigRepo != null ? systemConfigRepo.getAll() : Collections.emptyMap();

        // 1. Resolve GYM.* attributes
        String gymName = getConfig(sysConfigs, BrandConfigKeys.GYM_NAME.getKey(), BrandConfigKeys.GYM_NAME.getDefaultValue());
        String gymTimings = getConfig(sysConfigs, GymProfileConfigKeys.GYM_TIMINGS.getKey(), GymProfileConfigKeys.GYM_TIMINGS.getDefaultValue());
        String gymPhone = getConfig(sysConfigs, GymProfileConfigKeys.GYM_SUPPORT_PHONE.getKey(), GymProfileConfigKeys.GYM_SUPPORT_PHONE.getDefaultValue());
        String gymAddress = getConfig(sysConfigs, GymProfileConfigKeys.GYM_ADDRESS.getKey(), GymProfileConfigKeys.GYM_ADDRESS.getDefaultValue());
        String gymMapsUrl = getConfig(sysConfigs, GymProfileConfigKeys.GYM_GOOGLE_MAPS_URL.getKey(), GymProfileConfigKeys.GYM_GOOGLE_MAPS_URL.getDefaultValue());
        String gymChannelUrl = getConfig(sysConfigs, GymProfileConfigKeys.GYM_WHATSAPP_CHANNEL_URL.getKey(), GymProfileConfigKeys.GYM_WHATSAPP_CHANNEL_URL.getDefaultValue());
        String gymPaymentQr = getConfig(sysConfigs, GymProfileConfigKeys.GYM_UPI_QR_MEDIA_ID.getKey(), GymProfileConfigKeys.GYM_UPI_QR_MEDIA_ID.getDefaultValue());
        if (gymPaymentQr == null || gymPaymentQr.isBlank()) {
            gymPaymentQr = "upi://pay?pa=ironpulse@upi&pn=" + (gymName != null && !gymName.isBlank() ? gymName.replace(" ", "+") : "Fitness+Center");
        }

        for (String attr : requestedAttributes) {
            if ("GYM.name".equalsIgnoreCase(attr)) resolved.put(attr, gymName);
            if ("GYM.timings".equalsIgnoreCase(attr)) resolved.put(attr, gymTimings);
            if ("GYM.support_phone".equalsIgnoreCase(attr)) resolved.put(attr, gymPhone);
            if ("GYM.address".equalsIgnoreCase(attr)) resolved.put(attr, gymAddress);
            if ("GYM.google_maps_url".equalsIgnoreCase(attr)) resolved.put(attr, gymMapsUrl);
            if ("GYM.whatsapp_channel_url".equalsIgnoreCase(attr)) resolved.put(attr, gymChannelUrl);
            if ("GYM.payment_qr".equalsIgnoreCase(attr)) resolved.put(attr, gymPaymentQr);
        }

        // 2. Resolve GYM.plan_catalogue if requested
        if (requestedAttributes.stream().anyMatch(a -> a.equalsIgnoreCase("GYM.plan_catalogue"))) {
            List<PlanEntity> activePlans = planRepo != null ? planRepo.findAllActivePlans() : List.of();
            String catalogue = activePlans.stream()
                    .map(p -> "• " + p.getName() + ": ₹" + (p.getDefaultPrice() != null ? formatIndianAmount(p.getDefaultPrice()) : "0")
                            + " (" + (p.getDefaultDuration() != null ? p.getDefaultDuration() : 30) + " Days)")
                    .collect(Collectors.joining("\n"));
            if (catalogue.isBlank()) {
                catalogue = "• General Access: Contact front desk for membership packages.";
            }
            resolved.put("GYM.plan_catalogue", catalogue);
        }

        if (userId == null) {
            return resolved;
        }

        // 3. Resolve Member Plans (MEMBER.plans_breakdown, PLAN.*)
        List<UserPlanEntity> userPlans = userPlanRepo != null ? userPlanRepo.findByUserId(userId) : List.of();
        LocalDate today = LocalDate.now();
        Map<UUID, PlanEntity> planEntityMap = new HashMap<>();
        if (!userPlans.isEmpty() && planRepo != null) {
            List<UUID> planIds = userPlans.stream().map(UserPlanEntity::getPlanId).distinct().toList();
            planRepo.findByIds(planIds).forEach(p -> planEntityMap.put(p.getId(), p));
        }

        if (!userPlans.isEmpty()) {
            StringBuilder breakdown = new StringBuilder();

            // Find all plans that are about to expire:
            // 1. Prioritize active/upcoming plans (endDate >= today).
            //    - If any expire on or before the end of the current month, select all of them.
            //    - Else if any expire within 30 days, select all of them.
            //    - Else select all active plans on the earliest active expiry date.
            // 2. If all plans are in the past, select all plans on the most recently expired date.
            LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

            List<UserPlanEntity> activePlans = userPlans.stream()
                    .filter(up -> up.getEndDate() != null && !up.getEndDate().isBefore(today))
                    .sorted(Comparator.comparing(UserPlanEntity::getEndDate))
                    .toList();

            List<UserPlanEntity> targetPlans;
            if (!activePlans.isEmpty()) {
                List<UserPlanEntity> thisMonthPlans = activePlans.stream()
                        .filter(up -> !up.getEndDate().isAfter(endOfMonth))
                        .toList();

                if (!thisMonthPlans.isEmpty()) {
                    targetPlans = thisMonthPlans;
                } else {
                    List<UserPlanEntity> within30Days = activePlans.stream()
                            .filter(up -> ChronoUnit.DAYS.between(today, up.getEndDate()) <= 30)
                            .toList();

                    if (!within30Days.isEmpty()) {
                        targetPlans = within30Days;
                    } else {
                        LocalDate minEndDate = activePlans.get(0).getEndDate();
                        targetPlans = activePlans.stream()
                                .filter(up -> minEndDate.equals(up.getEndDate()))
                                .toList();
                    }
                }
            } else {
                Optional<LocalDate> maxExpiredDate = userPlans.stream()
                        .map(UserPlanEntity::getEndDate)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder());

                if (maxExpiredDate.isPresent()) {
                    targetPlans = userPlans.stream()
                            .filter(up -> maxExpiredDate.get().equals(up.getEndDate()))
                            .toList();
                } else {
                    targetPlans = List.of(userPlans.get(0));
                }
            }

            // Target plan names joined (e.g., "Gym, Personal Training")
            String combinedPlanNames = targetPlans.stream()
                    .map(up -> {
                        PlanEntity pe = planEntityMap.get(up.getPlanId());
                        return pe != null ? pe.getName() : null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining(", "));
            if (combinedPlanNames.isBlank()) {
                combinedPlanNames = "General Access";
            }

            LocalDate earliestExpiry = targetPlans.stream()
                    .map(UserPlanEntity::getEndDate)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            long daysLeftVal = earliestExpiry != null ? ChronoUnit.DAYS.between(today, earliestExpiry) : 0;

            for (UserPlanEntity up : userPlans) {
                PlanEntity pe = planEntityMap.get(up.getPlanId());
                String pName = pe != null ? pe.getName() : "Gym Service";
                String sDate = up.getStartDate() != null ? up.getStartDate().format(DATE_FMT) : "-";
                String eDate = up.getEndDate() != null ? up.getEndDate().format(DATE_FMT) : "-";

                long daysLeft = up.getEndDate() != null ? ChronoUnit.DAYS.between(today, up.getEndDate()) : 0;
                String daysStr = daysLeft < 0 ? "(Expired " + Math.abs(daysLeft) + "d ago)" : "(" + daysLeft + " days left)";

                if (!breakdown.isEmpty()) breakdown.append("\n");
                breakdown.append("• ").append(pName).append("\n  Period: ").append(sDate).append(" – ").append(eDate).append(" ").append(daysStr);
            }

            for (String attr : requestedAttributes) {
                if ("MEMBER.plans_breakdown".equalsIgnoreCase(attr)) {
                    resolved.put(attr, breakdown.toString());
                }
                if ("PLAN.name".equalsIgnoreCase(attr)) {
                    resolved.put(attr, combinedPlanNames);
                }
                if ("PLAN.expiry_date".equalsIgnoreCase(attr)) {
                    resolved.put(attr, earliestExpiry != null ? earliestExpiry.format(DATE_FMT) : "Active");
                }
                if ("PLAN.days_left".equalsIgnoreCase(attr)) {
                    resolved.put(attr, String.valueOf(Math.max(0, daysLeftVal)));
                }
            }
        } else {
            resolved.putIfAbsent("MEMBER.plans_breakdown", "• No active plans on record.");
            resolved.putIfAbsent("PLAN.name", "General Access");
            resolved.putIfAbsent("PLAN.expiry_date", "-");
            resolved.putIfAbsent("PLAN.days_left", "0");
        }

        // 4. Resolve Payment & Ledger (PAYMENT.*)
        if (!userPlans.isEmpty() && userPlanPaymentRepo != null && paymentRepo != null) {
            List<UUID> upIds = userPlans.stream().map(UserPlanEntity::getId).toList();
            List<UserPlanPaymentEntity> mappings = userPlanPaymentRepo.findByUserPlanIds(upIds);
            List<UUID> payIds = mappings.stream().map(UserPlanPaymentEntity::getPaymentId).distinct().toList();
            List<PaymentEntity> payments = paymentRepo.findByIds(payIds);

            BigDecimal totalPaid = BigDecimal.ZERO;
            BigDecimal totalDue = BigDecimal.ZERO;
            PaymentEntity nextUpcoming = null;
            PaymentEntity latestPaid = null;

            for (PaymentEntity p : payments) {
                BigDecimal pAmount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                BigDecimal pPaid = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;
                totalPaid = totalPaid.add(pPaid);

                BigDecimal remaining = pAmount.subtract(pPaid);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    totalDue = totalDue.add(remaining);
                    if (p.getDueDate() != null) {
                        if (nextUpcoming == null) {
                            nextUpcoming = p;
                        } else {
                            // Prioritize soonest upcoming installment (dueDate >= today)
                            boolean pIsUpcoming = !p.getDueDate().isBefore(today);
                            boolean nextIsUpcoming = nextUpcoming.getDueDate() != null && !nextUpcoming.getDueDate().isBefore(today);
                            if (pIsUpcoming && !nextIsUpcoming) {
                                nextUpcoming = p;
                            } else if (pIsUpcoming && nextIsUpcoming) {
                                if (p.getDueDate().isBefore(nextUpcoming.getDueDate())) {
                                    nextUpcoming = p;
                                }
                            } else if (!pIsUpcoming && !nextIsUpcoming) {
                                if (p.getDueDate().isAfter(nextUpcoming.getDueDate())) {
                                    nextUpcoming = p;
                                }
                            }
                        }
                    } else if (nextUpcoming == null) {
                        nextUpcoming = p;
                    }
                }

                if (pPaid.compareTo(BigDecimal.ZERO) > 0 && p.getPaidDate() != null) {
                    if (latestPaid == null || p.getPaidDate().isAfter(latestPaid.getPaidDate())) {
                        latestPaid = p;
                    }
                }
            }

            PlanEntity upcomingPaymentPlan = null;
            if (nextUpcoming != null) {
                final UUID nextPayId = nextUpcoming.getId();
                UUID targetUpId = mappings.stream()
                        .filter(m -> m.getPaymentId().equals(nextPayId))
                        .map(UserPlanPaymentEntity::getUserPlanId)
                        .findFirst().orElse(null);
                if (targetUpId != null) {
                    UserPlanEntity upe = userPlans.stream().filter(up -> up.getId().equals(targetUpId)).findFirst().orElse(null);
                    if (upe != null) {
                        upcomingPaymentPlan = planEntityMap.get(upe.getPlanId());
                    }
                }
            }

            // For payment reminders, resolve PLAN.name to the specific upcoming installment's plan (unlike renewals which combines all plans)
            boolean isPaymentReminder = requestedAttributes.stream().anyMatch(a ->
                    "PAYMENT.due_date".equalsIgnoreCase(a) || "PAYMENT.days_left".equalsIgnoreCase(a));
            if (isPaymentReminder && upcomingPaymentPlan != null && requestedAttributes.contains("PLAN.name")) {
                resolved.put("PLAN.name", upcomingPaymentPlan.getName());
            }

            // 4b. Collect overdue payments (past due with remaining unpaid balance)
            List<PaymentEntity> overduePayments = new ArrayList<>();
            BigDecimal totalOverdue = BigDecimal.ZERO;
            LocalDate earliestOverdueDueDate = null;

            for (PaymentEntity p : payments) {
                BigDecimal pAmount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                BigDecimal pPaid = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal remaining = pAmount.subtract(pPaid);

                if (remaining.compareTo(BigDecimal.ZERO) > 0 && p.getDueDate() != null && p.getDueDate().isBefore(today)) {
                    overduePayments.add(p);
                    totalOverdue = totalOverdue.add(remaining);
                    if (earliestOverdueDueDate == null || p.getDueDate().isBefore(earliestOverdueDueDate)) {
                        earliestOverdueDueDate = p.getDueDate();
                    }
                }
            }

            overduePayments.sort(Comparator.comparing(PaymentEntity::getDueDate));

            StringBuilder overdueBreakdown = new StringBuilder();
            if (!overduePayments.isEmpty()) {
                for (PaymentEntity p : overduePayments) {
                    BigDecimal pAmount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                    BigDecimal pPaid = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;
                    BigDecimal remaining = pAmount.subtract(pPaid);
                    String dStr = p.getDueDate() != null ? p.getDueDate().format(DATE_FMT) : "-";
                    if (!overdueBreakdown.isEmpty()) overdueBreakdown.append("\n");
                    overdueBreakdown.append("• ₹").append(formatIndianAmount(remaining)).append(" (Due: ").append(dStr).append(")");
                }
            } else {
                overdueBreakdown.append("• No overdue installments on record.");
            }

            for (String attr : requestedAttributes) {
                if ("PAYMENT.total_paid".equalsIgnoreCase(attr)) {
                    resolved.put(attr, "₹" + formatIndianAmount(totalPaid));
                }
                if ("PAYMENT.pending_balance".equalsIgnoreCase(attr)) {
                    if (requestedAttributes.contains("PAYMENT.overdue_breakdown")) {
                        resolved.put(attr, "₹" + formatIndianAmount(totalOverdue));
                    } else {
                        resolved.put(attr, "₹" + formatIndianAmount(totalDue));
                    }
                }
                if ("PAYMENT.overdue_breakdown".equalsIgnoreCase(attr)) {
                    resolved.put(attr, overdueBreakdown.toString());
                }
                if ("PAYMENT.overdue_since".equalsIgnoreCase(attr)) {
                    resolved.put(attr, earliestOverdueDueDate != null ? earliestOverdueDueDate.format(DATE_FMT) : "—");
                }
                if ("PAYMENT.next_due_summary".equalsIgnoreCase(attr)) {
                    if (totalDue.compareTo(BigDecimal.ZERO) > 0 && nextUpcoming != null) {
                        String dStr = nextUpcoming.getDueDate() != null ? nextUpcoming.getDueDate().format(DATE_FMT) : "Soon";
                        resolved.put(attr, "₹" + formatIndianAmount(totalDue) + " (Due: " + dStr + ")");
                    } else {
                        resolved.put(attr, "All Dues Cleared 🎉");
                    }
                }
                if ("PAYMENT.due_date".equalsIgnoreCase(attr)) {
                    String dStr = nextUpcoming != null && nextUpcoming.getDueDate() != null ? nextUpcoming.getDueDate().format(DATE_FMT) : "Immediate";
                    resolved.put(attr, dStr);
                }
                if ("PAYMENT.days_left".equalsIgnoreCase(attr)) {
                    long left = (nextUpcoming != null && nextUpcoming.getDueDate() != null)
                            ? ChronoUnit.DAYS.between(today, nextUpcoming.getDueDate())
                            : 0;
                    resolved.put(attr, String.valueOf(Math.max(0, left)));
                }
                if ("PAYMENT.receipt_id".equalsIgnoreCase(attr)) {
                    String rId = latestPaid != null && latestPaid.getTransactionId() != null && !latestPaid.getTransactionId().isBlank()
                            ? latestPaid.getTransactionId()
                            : ("REC-" + (latestPaid != null ? latestPaid.getId().toString().substring(0, 8).toUpperCase() : "001"));
                    resolved.put(attr, rId);
                }
                if ("PAYMENT.amount".equalsIgnoreCase(attr)) {
                    boolean isReceiptRequest = requestedAttributes.stream().anyMatch(a ->
                            "PAYMENT.receipt_id".equalsIgnoreCase(a) || "PAYMENT.paid_date".equalsIgnoreCase(a) || "PAYMENT.mode".equalsIgnoreCase(a));
                    if (isReceiptRequest && latestPaid != null && latestPaid.getPaidAmount() != null) {
                        resolved.put(attr, "₹" + formatIndianAmount(latestPaid.getPaidAmount()));
                    } else if (nextUpcoming != null) {
                        BigDecimal remaining = nextUpcoming.getAmount().subtract(nextUpcoming.getPaidAmount() != null ? nextUpcoming.getPaidAmount() : BigDecimal.ZERO);
                        BigDecimal amt = remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : nextUpcoming.getAmount();
                        resolved.put(attr, "₹" + formatIndianAmount(amt));
                    } else if (latestPaid != null && latestPaid.getPaidAmount() != null) {
                        resolved.put(attr, "₹" + formatIndianAmount(latestPaid.getPaidAmount()));
                    } else {
                        resolved.put(attr, "₹0");
                    }
                }
                if ("PAYMENT.mode".equalsIgnoreCase(attr)) {
                    resolved.put(attr, latestPaid != null && latestPaid.getPaymentMethod() != null ? latestPaid.getPaymentMethod().name() : "UPI");
                }
                if ("PAYMENT.paid_date".equalsIgnoreCase(attr)) {
                    String pDate = latestPaid != null && latestPaid.getPaidDate() != null ? latestPaid.getPaidDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);
                    resolved.put(attr, pDate);
                }
            }
        } else {
            resolved.putIfAbsent("PAYMENT.total_paid", "₹0");
            resolved.putIfAbsent("PAYMENT.pending_balance", "₹0");
            resolved.putIfAbsent("PAYMENT.overdue_breakdown", "• No overdue installments on record.");
            resolved.putIfAbsent("PAYMENT.overdue_since", "—");
            resolved.putIfAbsent("PAYMENT.next_due_summary", "All Dues Cleared 🎉");
            resolved.putIfAbsent("PAYMENT.due_date", "-");
            resolved.putIfAbsent("PAYMENT.days_left", "0");
            resolved.putIfAbsent("PAYMENT.receipt_id", "REC-001");
            resolved.putIfAbsent("PAYMENT.amount", "₹0");
            resolved.putIfAbsent("PAYMENT.mode", "Direct");
            resolved.putIfAbsent("PAYMENT.paid_date", LocalDate.now().format(DATE_FMT));
        }

        return resolved;
    }

    private String getConfig(Map<String, String> configs, String key, String defaultVal) {
        if (configs != null && configs.containsKey(key)) {
            String val = configs.get(key);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return defaultVal;
    }

    public static String formatIndianAmount(BigDecimal amount) {
        if (amount == null) return "0";
        BigDecimal stripped = amount.stripTrailingZeros();
        boolean hasFraction = stripped.scale() > 0;
        long integerPart = stripped.longValue();
        String intStr = String.valueOf(Math.abs(integerPart));

        StringBuilder sb = new StringBuilder();
        if (intStr.length() <= 3) {
            sb.append(intStr);
        } else {
            String lastThree = intStr.substring(intStr.length() - 3);
            String rest = intStr.substring(0, intStr.length() - 3);
            StringBuilder restGrouped = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                if (i > 0 && (rest.length() - i) % 2 == 0) {
                    restGrouped.append(",");
                }
                restGrouped.append(rest.charAt(i));
            }
            sb.append(restGrouped).append(",").append(lastThree);
        }

        if (integerPart < 0) {
            sb.insert(0, "-");
        }

        if (hasFraction) {
            String fullStr = stripped.toPlainString();
            int dotIdx = fullStr.indexOf('.');
            if (dotIdx != -1) {
                sb.append(".").append(fullStr.substring(dotIdx + 1));
            }
        }

        return sb.toString();
    }
}
