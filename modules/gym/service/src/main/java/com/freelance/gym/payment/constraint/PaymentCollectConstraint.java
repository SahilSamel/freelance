package com.freelance.gym.payment.constraint;

import com.freelance.gym.payment.dto.PaymentCollectRequestDTO;
import com.freelance.gym.payment.entity.PaymentEntity;
import com.freelance.gym.payment.repository.PaymentRepository;
import com.freelance.keys.gym.payment.PaymentConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class PaymentCollectConstraint extends AbstractConstraint {

    private final PaymentRepository paymentRepository;

    public PaymentCollectConstraint() {
        this(new PaymentRepository());
    }

    public PaymentCollectConstraint(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void checkConstraints(PaymentCollectRequestDTO request) {
        if (request == null || request.paymentId() == null) {
            return;
        }

        PaymentEntity payment = paymentRepository.get(request.paymentId());
        if (payment == null) {
            raiseConstraintError(PaymentConstraintErrors.PAYMENT_NOT_FOUND);
            return;
        }

        if (payment.getPaidAmount() != null && payment.getPaidAmount().compareTo(payment.getAmount()) >= 0) {
            raiseConstraintError(PaymentConstraintErrors.PAID_PAYMENT_CANNOT_BE_MODIFIED);
        }
    }
}
