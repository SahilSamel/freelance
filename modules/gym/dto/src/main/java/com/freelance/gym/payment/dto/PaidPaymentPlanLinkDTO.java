package com.freelance.gym.payment.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record PaidPaymentPlanLinkDTO(
        UUID paymentId,
        List<Integer> planIndices
) implements Serializable {
}
