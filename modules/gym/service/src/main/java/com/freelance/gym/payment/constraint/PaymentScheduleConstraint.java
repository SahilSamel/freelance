package com.freelance.gym.payment.constraint;

import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.keys.gym.payment.PaymentConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PaymentScheduleConstraint extends AbstractConstraint {

    private final PaymentRepository paymentRepository;

    public PaymentScheduleConstraint() {
        this(new PaymentRepository());
    }

    public PaymentScheduleConstraint(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void checkPaidPaymentsImmutable(List<UUID> paymentIdsToModify) {
        if (paymentIdsToModify == null || paymentIdsToModify.isEmpty()) {
            return;
        }

        List<PaymentEntity> payments = paymentRepository.findByIds(paymentIdsToModify);
        for (PaymentEntity p : payments) {
            boolean isPaid = (p.getPaidAmount() != null && p.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)
                    || (p.getTransactionId() != null && !p.getTransactionId().isBlank())
                    || (p.getPaidDate() != null);
            if (isPaid) {
                raiseConstraintError(PaymentConstraintErrors.PAID_PAYMENT_CANNOT_BE_MODIFIED);
            }
        }
    }
}
