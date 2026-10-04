package com.freelance.gym.payment;

import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.assembler.PaymentAssembler;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.entity.UserPlanPaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.gym.payment.repository.UserPlanPaymentRepository;
import com.freelance.gym.payment.validation.PaymentScheduleValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserPlanPaymentRepository userPlanPaymentRepository;
    private final PaymentAssembler assembler;

    public PaymentService() {
        this(new PaymentRepository(), new UserPlanPaymentRepository(), new PaymentAssembler());
    }

    public PaymentService(
            PaymentRepository paymentRepository,
            UserPlanPaymentRepository userPlanPaymentRepository,
            PaymentAssembler assembler
    ) {
        this.paymentRepository = paymentRepository;
        this.userPlanPaymentRepository = userPlanPaymentRepository;
        this.assembler = assembler;
    }

    public List<PaymentResponseDTO> createInstallments(List<UserPlanResponseDTO> userPlans, List<PaymentScheduleDTO> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }

        new PaymentScheduleValidation().validateScheduleList(payments);

        List<UUID> planIds = (userPlans != null)
                ? userPlans.stream().map(UserPlanResponseDTO::id).toList()
                : List.of();

        List<PaymentResponseDTO> responseList = new ArrayList<>();

        for (PaymentScheduleDTO dto : payments) {
            PaymentEntity entity = assembler.toEntity(dto);
            paymentRepository.persist(entity);

            if (!planIds.isEmpty()) {
                for (UUID userPlanId : planIds) {
                    UserPlanPaymentEntity mapping = new UserPlanPaymentEntity(userPlanId, entity.getId());
                    userPlanPaymentRepository.persist(mapping);
                }
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
                .filter(p -> p.getTransactionId() == null || p.getTransactionId().isBlank())
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
                .filter(p -> p.getTransactionId() != null && !p.getTransactionId().isBlank())
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

    public void relinkPaidPayments(List<UUID> oldUserPlanIds, List<UserPlanResponseDTO> newUserPlans) {
        List<PaymentEntity> paidPayments = findPaidPaymentsByUserPlanIds(oldUserPlanIds);
        linkPaidPaymentsToNewPlans(paidPayments, newUserPlans);
    }
}
