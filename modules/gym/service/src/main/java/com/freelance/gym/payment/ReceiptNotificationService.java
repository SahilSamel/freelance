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
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
import com.freelance.gym.pdf.ReceiptPdfGenerator;
import com.freelance.gym.pdf.ReceiptPdfGenerator.ReceiptInstallmentItem;
import com.freelance.gym.pdf.ReceiptPdfGenerator.ReceiptModel;
import com.freelance.gym.pdf.ReceiptPdfGenerator.ReceiptPlanItem;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.keys.branding.BrandConfigKeys;
import com.freelance.keys.gym.GymProfileConfigKeys;
import com.freelance.user.UserDTO;
import com.freelance.user.read.UserReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating PDF receipt generation, media storage, and instant WhatsApp alert dispatch.
 */
public class ReceiptNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptNotificationService.class);

    private final SystemConfigurationRepository systemConfigRepo;
    private final UserReadService userReadService;
    private final UserContactService userContactService;
    private final UserPlanRepository userPlanRepo;
    private final PlanRepository planRepo;
    private final PaymentRepository paymentRepo;
    private final UserPlanPaymentRepository userPlanPaymentRepo;
    private final AlertMediaService mediaService;
    private final AlertsEngine alertsEngine;

    public ReceiptNotificationService() {
        this(
                new SystemConfigurationRepository(),
                new UserReadService(),
                new UserContactService(),
                new UserPlanRepository(),
                new PlanRepository(),
                new PaymentRepository(),
                new UserPlanPaymentRepository(),
                new AlertMediaService(),
                AlertsEngine.getInstance()
        );
    }

    public ReceiptNotificationService(
            SystemConfigurationRepository systemConfigRepo,
            UserReadService userReadService,
            UserContactService userContactService,
            UserPlanRepository userPlanRepo,
            PlanRepository planRepo,
            PaymentRepository paymentRepo,
            UserPlanPaymentRepository userPlanPaymentRepo,
            AlertMediaService mediaService,
            AlertsEngine alertsEngine
    ) {
        this.systemConfigRepo = systemConfigRepo;
        this.userReadService = userReadService;
        this.userContactService = userContactService;
        this.userPlanRepo = userPlanRepo;
        this.planRepo = planRepo;
        this.paymentRepo = paymentRepo;
        this.userPlanPaymentRepo = userPlanPaymentRepo;
        this.mediaService = mediaService;
        this.alertsEngine = alertsEngine;
    }

    public void generateAndDispatchReceipt(
            UUID userId,
            UUID paymentId,
            BigDecimal paidAmount,
            String paymentMethod,
            String transactionId,
            LocalDate paidDate,
            List<UserPlanResponseDTO> coveredPlans
    ) {
        if (userId == null || paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        try {
            // 1. Gym Profile & Branding Configuration
            Map<String, String> configs = systemConfigRepo != null ? systemConfigRepo.getAll() : Collections.emptyMap();
            String gymName = configs.getOrDefault(BrandConfigKeys.GYM_NAME.getKey(), BrandConfigKeys.GYM_NAME.getDefaultValue());
            String gymAddress = configs.getOrDefault(GymProfileConfigKeys.GYM_ADDRESS.getKey(), GymProfileConfigKeys.GYM_ADDRESS.getDefaultValue());
            String gymBillingAddress = configs.getOrDefault(GymProfileConfigKeys.GYM_BILLING_ADDRESS.getKey(), GymProfileConfigKeys.GYM_BILLING_ADDRESS.getDefaultValue());
            String gymGstin = configs.getOrDefault(GymProfileConfigKeys.GYM_GSTIN.getKey(), GymProfileConfigKeys.GYM_GSTIN.getDefaultValue());
            String gymPhone = configs.getOrDefault(GymProfileConfigKeys.GYM_SUPPORT_PHONE.getKey(), GymProfileConfigKeys.GYM_SUPPORT_PHONE.getDefaultValue());
            String gymLogoUrl = configs.getOrDefault(BrandConfigKeys.GYM_LOGO_URL.getKey(), BrandConfigKeys.GYM_LOGO_URL.getDefaultValue());

            // 2. Member User Details & Contact Information
            List<UserDTO> users = userReadService != null ? userReadService.readUsersByIds(List.of(userId)) : List.of();
            UserDTO user = !users.isEmpty() ? users.get(0) : null;
            String memberName = user != null ? (user.firstName() + " " + (user.lastName() != null ? user.lastName() : "")).trim() : "Member";
            String memberUsername = user != null ? user.username() : "";

            String memberPhone = "";
            if (userContactService != null) {
                Optional<UserContactDTO> contact = userContactService.findByUserAndChannel(userId, AlertChannel.WHATSAPP);
                memberPhone = contact.map(UserContactDTO::contactValue).orElse("");
            }

            // 3. Resolve All Linked Plans for this Payment Group
            Set<UUID> linkedPlanIds = new LinkedHashSet<>();
            if (paymentId != null && userPlanPaymentRepo != null) {
                List<UserPlanPaymentEntity> mappings = userPlanPaymentRepo.findByPaymentId(paymentId);
                for (UserPlanPaymentEntity m : mappings) {
                    if (m.getUserPlanId() != null) {
                        linkedPlanIds.add(m.getUserPlanId());
                    }
                }
            }

            if (linkedPlanIds.isEmpty() && coveredPlans != null) {
                for (UserPlanResponseDTO cp : coveredPlans) {
                    if (cp.id() != null) {
                        linkedPlanIds.add(cp.id());
                    }
                }
            }

            if (linkedPlanIds.isEmpty() && userPlanRepo != null) {
                List<UserPlanEntity> userPlans = userPlanRepo.findByUserId(userId);
                for (UserPlanEntity up : userPlans) {
                    linkedPlanIds.add(up.getId());
                }
            }

            // 4. Build Plans Items Table
            List<ReceiptPlanItem> planItems = new ArrayList<>();
            Map<UUID, UserPlanResponseDTO> coveredPlansMap = (coveredPlans != null)
                    ? coveredPlans.stream().filter(p -> p.id() != null).collect(Collectors.toMap(UserPlanResponseDTO::id, p -> p, (a, b) -> a))
                    : Collections.emptyMap();

            for (UUID upId : linkedPlanIds) {
                UserPlanResponseDTO cp = coveredPlansMap.get(upId);
                if (cp != null) {
                    PlanEntity pe = (planRepo != null && cp.planId() != null) ? planRepo.get(cp.planId()) : null;
                    BigDecimal price = pe != null ? pe.getDefaultPrice() : BigDecimal.ZERO;
                    planItems.add(new ReceiptPlanItem(cp.planName(), cp.startDate(), cp.endDate(), price));
                } else if (userPlanRepo != null) {
                    UserPlanEntity upe = userPlanRepo.get(upId);
                    if (upe != null) {
                        PlanEntity pe = (planRepo != null && upe.getPlanId() != null) ? planRepo.get(upe.getPlanId()) : null;
                        String pName = pe != null ? pe.getName() : "Membership Plan";
                        BigDecimal price = pe != null ? pe.getDefaultPrice() : BigDecimal.ZERO;
                        planItems.add(new ReceiptPlanItem(pName, upe.getStartDate(), upe.getEndDate(), price));
                    }
                }
            }

            // 5. Resolve All Sibling Installments for this Linked Package
            List<UUID> siblingPaymentIds = new ArrayList<>();
            if (userPlanPaymentRepo != null && !linkedPlanIds.isEmpty()) {
                List<UserPlanPaymentEntity> allGroupMappings = userPlanPaymentRepo.findByUserPlanIds(new ArrayList<>(linkedPlanIds));
                for (UserPlanPaymentEntity m : allGroupMappings) {
                    if (m.getPaymentId() != null && !siblingPaymentIds.contains(m.getPaymentId())) {
                        siblingPaymentIds.add(m.getPaymentId());
                    }
                }
            }

            if (paymentId != null && !siblingPaymentIds.contains(paymentId)) {
                siblingPaymentIds.add(paymentId);
            }

            List<PaymentEntity> paymentEntities = new ArrayList<>();
            if (paymentRepo != null && !siblingPaymentIds.isEmpty()) {
                paymentEntities = new ArrayList<>(paymentRepo.findByIds(siblingPaymentIds));
            }

            // Ensure the currently paid payment is updated in memory if state was just transitioned
            boolean foundCurrent = false;
            for (PaymentEntity p : paymentEntities) {
                if (p.getId().equals(paymentId)) {
                    foundCurrent = true;
                    if (p.getPaidAmount() == null || p.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        p.setPaidAmount(paidAmount);
                    }
                    if (p.getPaidDate() == null && paidDate != null) {
                        p.setPaidDate(paidDate);
                    }
                    if ((p.getPaymentMethod() == null || p.getPaymentMethod() == PaymentMethod.NONE) && paymentMethod != null) {
                        try {
                            p.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
                        } catch (Exception ignored) {}
                    }
                    if (p.getTransactionId() == null && transactionId != null) {
                        p.setTransactionId(transactionId);
                    }
                }
            }

            if (!foundCurrent && paymentId != null) {
                PaymentMethod pm = PaymentMethod.NONE;
                if (paymentMethod != null) {
                    try {
                        pm = PaymentMethod.valueOf(paymentMethod);
                    } catch (Exception ignored) {}
                }
                PaymentEntity currentFallback = new PaymentEntity(
                        paymentId,
                        paidAmount,
                        paidAmount,
                        paidDate != null ? paidDate : LocalDate.now(),
                        paidDate != null ? paidDate : LocalDate.now(),
                        pm,
                        transactionId,
                        null
                );
                paymentEntities.add(currentFallback);
            }

            // Sort payments by due date ascending
            paymentEntities.sort(Comparator.comparing(
                    p -> p.getDueDate() != null ? p.getDueDate() : LocalDate.MIN
            ));

            // 6. Build 6-Column Installment Breakdown & Calculate Totals
            List<ReceiptInstallmentItem> installmentItems = new ArrayList<>();
            BigDecimal totalBilled = BigDecimal.ZERO;
            BigDecimal totalPaid = BigDecimal.ZERO;
            LocalDate nextDueDate = null;

            int instIndex = 1;
            for (PaymentEntity p : paymentEntities) {
                BigDecimal instAmount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                BigDecimal instPaid = p.getPaidAmount() != null ? p.getPaidAmount() : BigDecimal.ZERO;
                boolean isPaid = instPaid.compareTo(BigDecimal.ZERO) > 0;

                totalBilled = totalBilled.add(instAmount);
                totalPaid = totalPaid.add(instPaid);

                if (!isPaid && nextDueDate == null && p.getDueDate() != null) {
                    nextDueDate = p.getDueDate();
                }

                String method = p.getPaymentMethod() != null && p.getPaymentMethod() != PaymentMethod.NONE
                        ? p.getPaymentMethod().name()
                        : (paymentMethod != null ? paymentMethod : "UPI");

                installmentItems.add(new ReceiptInstallmentItem(
                        instIndex++,
                        instAmount,
                        instPaid,
                        p.getDueDate(),
                        p.getPaidDate(),
                        method,
                        p.getTransactionId(),
                        isPaid ? "PAID" : "PENDING"
                ));
            }

            if (totalBilled.compareTo(BigDecimal.ZERO) <= 0) {
                totalBilled = totalPaid;
            }
            BigDecimal balanceDue = totalBilled.subtract(totalPaid).max(BigDecimal.ZERO);

            // 7. Receipt Number Generation
            String rNum = (transactionId != null && !transactionId.isBlank())
                    ? transactionId
                    : ("REC-" + (paymentId != null ? paymentId.toString().substring(0, 8).toUpperCase() : UUID.randomUUID().toString().substring(0, 8).toUpperCase()));

            ReceiptModel model = new ReceiptModel(
                    gymName,
                    gymAddress,
                    gymPhone,
                    rNum,
                    LocalDate.now(),
                    memberName,
                    memberPhone,
                    memberUsername,
                    planItems,
                    installmentItems,
                    totalBilled,
                    totalPaid,
                    balanceDue,
                    nextDueDate,
                    paymentMethod != null ? paymentMethod : "UPI",
                    gymBillingAddress,
                    gymGstin,
                    gymLogoUrl
            );

            // 8. Generate PDF and Upload Media
            byte[] pdfBytes = ReceiptPdfGenerator.generateReceiptPdf(model);
            String base64 = Base64.getEncoder().encodeToString(pdfBytes);

            String firstName = (user != null && user.firstName() != null && !user.firstName().isBlank())
                    ? user.firstName().trim()
                    : (memberName != null && !memberName.isBlank() ? memberName.split("\\s+")[0].trim() : "Member");
            String sendDateStr = LocalDate.now().toString();
            String fileName = "Payment_Receipt_" + firstName + "_" + sendDateStr + ".pdf";

            MediaUploadResponseDTO uploaded = mediaService.uploadMedia(new MediaUploadRequestDTO(
                    fileName,
                    "application/pdf",
                    base64
            ));

            logger.info("[ReceiptNotificationService] Generated & uploaded receipt PDF '{}' (Media ID: {}) for user {}",
                    fileName, uploaded != null ? uploaded.mediaId() : "N/A", userId);

            String mediaUrl = uploaded != null ? uploaded.mediaUrl() : null;
            String mediaId = uploaded != null ? uploaded.mediaId() : null;

            // 9. Dispatch Instant Receipt WhatsApp Template
            alertsEngine.emitAlert(new PaymentReceiptInstantTemplate(), userId, mediaUrl, mediaId, "DOCUMENT");
        } catch (Exception e) {
            logger.warn("[ReceiptNotificationService] Failed to generate/upload receipt PDF for user {}: {}", userId, e.getMessage(), e);
            // Fallback: emit alert text anyway
            alertsEngine.emitAlert(new PaymentReceiptInstantTemplate(), userId);
        }
    }
}
