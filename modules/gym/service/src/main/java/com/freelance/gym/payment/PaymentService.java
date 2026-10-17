package com.freelance.gym.payment;

import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.assembler.PaymentAssembler;
import com.freelance.gym.payment.dto.PaymentCollectRequestDTO;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
import com.freelance.gym.payment.constraint.PaymentCollectConstraint;
import com.freelance.gym.payment.validation.PaymentCollectValidation;
import com.freelance.gym.payment.validation.PaymentScheduleValidation;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.gym.payment.dto.PaymentReceiptSendRequestDTO;
import com.freelance.gym.payment.dto.PaymentReceiptSendResponseDTO;
import com.freelance.keys.gym.payment.PaymentConstraintErrors;
import com.freelance.keys.gym.payment.PaymentValidationErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserPlanPaymentRepository userPlanPaymentRepository;
    private final PaymentAssembler assembler;
    private final UserPlanRepository userPlanRepository;
    private final PlanRepository planRepository;
    private final ReceiptNotificationService receiptNotificationService;

    public PaymentService() {
        this(new PaymentRepository(), new UserPlanPaymentRepository(), new PaymentAssembler(), new UserPlanRepository(), new PlanRepository(), new ReceiptNotificationService());
    }

    public PaymentService(
            PaymentRepository paymentRepository,
            UserPlanPaymentRepository userPlanPaymentRepository,
            PaymentAssembler assembler
    ) {
        this(paymentRepository, userPlanPaymentRepository, assembler, new UserPlanRepository(), new PlanRepository(), new ReceiptNotificationService());
    }

    public PaymentService(
            PaymentRepository paymentRepository,
            UserPlanPaymentRepository userPlanPaymentRepository,
            PaymentAssembler assembler,
            UserPlanRepository userPlanRepository,
            PlanRepository planRepository
    ) {
        this(paymentRepository, userPlanPaymentRepository, assembler, userPlanRepository, planRepository, new ReceiptNotificationService());
    }

    public PaymentService(
            PaymentRepository paymentRepository,
            UserPlanPaymentRepository userPlanPaymentRepository,
            PaymentAssembler assembler,
            UserPlanRepository userPlanRepository,
            PlanRepository planRepository,
            ReceiptNotificationService receiptNotificationService
    ) {
        this.paymentRepository = paymentRepository;
        this.userPlanPaymentRepository = userPlanPaymentRepository;
        this.assembler = assembler;
        this.userPlanRepository = userPlanRepository;
        this.planRepository = planRepository;
        this.receiptNotificationService = receiptNotificationService;
    }

    public List<PaymentResponseDTO> createInstallments(List<UserPlanResponseDTO> userPlans, List<PaymentScheduleDTO> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }

        new PaymentScheduleValidation().validateScheduleList(payments);

        List<UUID> allPlanIds = (userPlans != null)
                ? userPlans.stream().map(UserPlanResponseDTO::id).toList()
                : List.of();

        List<PaymentResponseDTO> responseList = new ArrayList<>();

        for (PaymentScheduleDTO dto : payments) {
            PaymentEntity entity = assembler.toEntity(dto);
            paymentRepository.persist(entity);

            List<UUID> targetPlanIds = new ArrayList<>();
            if (dto.planIndices() != null && !dto.planIndices().isEmpty() && userPlans != null) {
                for (Integer idx : dto.planIndices()) {
                    if (idx != null && idx >= 0 && idx < userPlans.size()) {
                        targetPlanIds.add(userPlans.get(idx).id());
                    }
                }
            } else {
                targetPlanIds.addAll(allPlanIds);
            }

            for (UUID userPlanId : targetPlanIds) {
                UserPlanPaymentEntity mapping = new UserPlanPaymentEntity(userPlanId, entity.getId());
                userPlanPaymentRepository.persist(mapping);
            }

            responseList.add(assembler.fromEntity(entity, userPlans != null ? userPlans : List.of()));
        }

        return responseList;
    }

    public void deletePendingPaymentsByUserPlanIds(List<UUID> userPlanIds) {
        if (userPlanIds == null || userPlanIds.isEmpty()) {
            return;
        }

        List<UserPlanPaymentEntity> mappings = userPlanPaymentRepository.findByUserPlanIds(userPlanIds);
        if (mappings.isEmpty()) {
            return;
        }

        List<UUID> paymentIds = mappings.stream().map(UserPlanPaymentEntity::getPaymentId).distinct().toList();
        List<PaymentEntity> payments = paymentRepository.findByIds(paymentIds);

        List<UUID> pendingPaymentIds = payments.stream()
                .filter(p -> (p.getPaidAmount() == null || p.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0)
                        && (p.getTransactionId() == null || p.getTransactionId().isBlank())
                        && p.getPaidDate() == null)
                .map(PaymentEntity::getId)
                .toList();

        if (!pendingPaymentIds.isEmpty()) {
            userPlanPaymentRepository.deleteByPaymentIds(pendingPaymentIds);
            paymentRepository.deletePendingByIds(pendingPaymentIds);
        }
    }

    public List<PaymentEntity> findPaidPaymentsByUserPlanIds(List<UUID> userPlanIds) {
        if (userPlanIds == null || userPlanIds.isEmpty()) {
            return List.of();
        }

        List<UserPlanPaymentEntity> mappings = userPlanPaymentRepository.findByUserPlanIds(userPlanIds);
        if (mappings.isEmpty()) {
            return List.of();
        }

        List<UUID> paymentIds = mappings.stream().map(UserPlanPaymentEntity::getPaymentId).distinct().toList();
        List<PaymentEntity> payments = paymentRepository.findByIds(paymentIds);

        return payments.stream()
                .filter(p -> (p.getPaidAmount() != null && p.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)
                        || (p.getTransactionId() != null && !p.getTransactionId().isBlank())
                        || p.getPaidDate() != null)
                .toList();
    }

    public void linkPaidPaymentsToNewPlans(List<PaymentEntity> paidPayments, List<UserPlanResponseDTO> newUserPlans) {
        if (paidPayments == null || paidPayments.isEmpty() || newUserPlans == null || newUserPlans.isEmpty()) {
            return;
        }

        for (PaymentEntity paid : paidPayments) {
            for (UserPlanResponseDTO plan : newUserPlans) {
                UserPlanPaymentEntity mapping = new UserPlanPaymentEntity(plan.id(), paid.getId());
                userPlanPaymentRepository.persist(mapping);
            }
        }
    }

    public void linkPaidPaymentsPrecisely(
            List<PaymentEntity> paidPayments,
            List<UserPlanResponseDTO> newPlans,
            List<UserPlanEntity> oldUserPlans,
            Map<UUID, Set<UUID>> oldPlansByPaymentId,
            Map<UUID, Integer> oldPlanIndexMap
    ) {
        linkPaidPaymentsPrecisely(paidPayments, newPlans, oldUserPlans, oldPlansByPaymentId, oldPlanIndexMap, null);
    }

    public void linkPaidPaymentsPrecisely(
            List<PaymentEntity> paidPayments,
            List<UserPlanResponseDTO> newPlans,
            List<UserPlanEntity> oldUserPlans,
            Map<UUID, Set<UUID>> oldPlansByPaymentId,
            Map<UUID, Integer> oldPlanIndexMap,
            List<com.freelance.gym.payment.dto.PaidPaymentPlanLinkDTO> explicitLinks
    ) {
        if (paidPayments == null || paidPayments.isEmpty() || newPlans == null || newPlans.isEmpty()) {
            return;
        }

        Map<UUID, List<Integer>> explicitMap = new HashMap<>();
        if (explicitLinks != null) {
            for (com.freelance.gym.payment.dto.PaidPaymentPlanLinkDTO link : explicitLinks) {
                if (link.paymentId() != null && link.planIndices() != null) {
                    explicitMap.put(link.paymentId(), link.planIndices());
                }
            }
        }

        for (PaymentEntity paid : paidPayments) {
            Set<UUID> linkedNewPlanIds = new HashSet<>();

            if (explicitMap.containsKey(paid.getId())) {
                List<Integer> indices = explicitMap.get(paid.getId());
                for (Integer idx : indices) {
                    if (idx != null && idx >= 0 && idx < newPlans.size()) {
                        linkedNewPlanIds.add(newPlans.get(idx).id());
                    }
                }
            } else {
                Set<UUID> associatedOldPlanIds = oldPlansByPaymentId != null
                        ? oldPlansByPaymentId.getOrDefault(paid.getId(), Set.of())
                        : Set.of();

                for (UUID oldPlanId : associatedOldPlanIds) {
                    Integer idx = oldPlanIndexMap != null ? oldPlanIndexMap.get(oldPlanId) : null;
                    if (idx != null && idx < newPlans.size()) {
                        linkedNewPlanIds.add(newPlans.get(idx).id());
                    } else if (oldUserPlans != null) {
                        UserPlanEntity oldP = oldUserPlans.stream()
                                .filter(p -> p.getId().equals(oldPlanId))
                                .findFirst()
                                .orElse(null);
                        if (oldP != null) {
                            for (UserPlanResponseDTO np : newPlans) {
                                if (np.planId().equals(oldP.getPlanId()) &&
                                    ((oldP.getStartDate() == null && np.startDate() == null) ||
                                     (oldP.getStartDate() != null && oldP.getStartDate().equals(np.startDate())))) {
                                    linkedNewPlanIds.add(np.id());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (linkedNewPlanIds.isEmpty() && !newPlans.isEmpty()) {
                linkedNewPlanIds.add(newPlans.get(0).id());
            }

            for (UUID newPlanId : linkedNewPlanIds) {
                userPlanPaymentRepository.persist(new UserPlanPaymentEntity(newPlanId, paid.getId()));
            }
        }
    }

    public void relinkPaidPayments(List<UUID> oldUserPlanIds, List<UserPlanResponseDTO> newUserPlans) {
        List<PaymentEntity> paidPayments = findPaidPaymentsByUserPlanIds(oldUserPlanIds);
        linkPaidPaymentsToNewPlans(paidPayments, newUserPlans);
    }

    public PaymentResponseDTO collectPayment(PaymentCollectRequestDTO request) {
        new PaymentCollectValidation().validateRequest(request);
        new PaymentCollectConstraint(paymentRepository).checkConstraints(request);

        PaymentEntity payment = paymentRepository.get(request.paymentId());
        payment.setPaidAmount(request.paidAmount());
        payment.setPaidDate(request.paidDate());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setTransactionId((request.transactionId() != null && !request.transactionId().isBlank())
                ? request.transactionId().trim()
                : null);
        if (request.notes() != null) {
            payment.setNotes(request.notes().trim());
        }

        paymentRepository.merge(payment);

        List<UserPlanResponseDTO> coveredPlans = new ArrayList<>();
        if (userPlanRepository != null && planRepository != null) {
            List<UserPlanPaymentEntity> mappings = userPlanPaymentRepository.findByPaymentId(payment.getId());
            for (UserPlanPaymentEntity m : mappings) {
                UserPlanEntity up = userPlanRepository.get(m.getUserPlanId());
                if (up != null) {
                    PlanEntity plan = planRepository.get(up.getPlanId());
                    coveredPlans.add(new UserPlanResponseDTO(
                            up.getId(),
                            up.getUserId(),
                            up.getPlanId(),
                            plan != null ? plan.getName() : "Unknown Plan",
                            up.getStartDate(),
                            up.getEndDate()
                    ));
                }
            }
        }

        PaymentResponseDTO response = assembler.fromEntity(payment, coveredPlans);

        if (payment.getPaidAmount() != null && payment.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 && !coveredPlans.isEmpty()) {
            UUID memberUserId = coveredPlans.get(0).userId();
            if (receiptNotificationService != null) {
                receiptNotificationService.generateAndDispatchReceipt(
                        memberUserId,
                        payment.getId(),
                        payment.getPaidAmount(),
                        payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "UPI",
                        payment.getTransactionId(),
                        payment.getPaidDate(),
                        coveredPlans
                );
            }
        }

        return response;
    }

    public PaymentReceiptSendResponseDTO sendReceipt(PaymentReceiptSendRequestDTO request) {
        if (request == null || request.paymentId() == null) {
            throw new ConstraintException(PaymentValidationErrors.PAYMENT_ID_REQUIRED);
        }

        PaymentEntity payment = paymentRepository.get(request.paymentId());
        if (payment == null) {
            throw new ConstraintException(PaymentConstraintErrors.PAYMENT_NOT_FOUND);
        }

        List<UserPlanResponseDTO> coveredPlans = new ArrayList<>();
        UUID resolvedUserId = request.userId();

        if (userPlanPaymentRepository != null) {
            List<UserPlanPaymentEntity> mappings = userPlanPaymentRepository.findByPaymentId(payment.getId());
            for (UserPlanPaymentEntity m : mappings) {
                if (userPlanRepository != null) {
                    UserPlanEntity up = userPlanRepository.get(m.getUserPlanId());
                    if (up != null) {
                        if (resolvedUserId == null) {
                            resolvedUserId = up.getUserId();
                        }
                        PlanEntity plan = (planRepository != null && up.getPlanId() != null) ? planRepository.get(up.getPlanId()) : null;
                        coveredPlans.add(new UserPlanResponseDTO(
                                up.getId(),
                                up.getUserId(),
                                up.getPlanId(),
                                plan != null ? plan.getName() : "Unknown Plan",
                                up.getStartDate(),
                                up.getEndDate()
                        ));
                    }
                }
            }
        }

        if (resolvedUserId != null && coveredPlans.isEmpty() && userPlanRepository != null) {
            List<UserPlanEntity> userPlans = userPlanRepository.findByUserId(resolvedUserId);
            for (UserPlanEntity up : userPlans) {
                PlanEntity plan = (planRepository != null && up.getPlanId() != null) ? planRepository.get(up.getPlanId()) : null;
                coveredPlans.add(new UserPlanResponseDTO(
                        up.getId(),
                        up.getUserId(),
                        up.getPlanId(),
                        plan != null ? plan.getName() : "Unknown Plan",
                        up.getStartDate(),
                        up.getEndDate()
                ));
            }
        }

        if (resolvedUserId == null) {
            throw new ConstraintException(PaymentConstraintErrors.USER_PLAN_PAYMENT_MAPPING_FAILED);
        }

        BigDecimal paidAmount = (payment.getPaidAmount() != null && payment.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)
                ? payment.getPaidAmount()
                : payment.getAmount();

        if (receiptNotificationService != null) {
            receiptNotificationService.generateAndDispatchReceipt(
                    resolvedUserId,
                    payment.getId(),
                    paidAmount,
                    payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "UPI",
                    payment.getTransactionId(),
                    payment.getPaidDate() != null ? payment.getPaidDate() : payment.getDueDate(),
                    coveredPlans
            );
        }

        return new PaymentReceiptSendResponseDTO(payment.getId(), true, "Receipt dispatched successfully");
    }
}
