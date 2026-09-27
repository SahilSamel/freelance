package com.freelance.gym.payment.assembler;

import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.gym.payment.entity.PaymentEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PaymentAssembler {

    public PaymentEntity toEntity(PaymentScheduleDTO dto) {
        if (dto == null) {
            return null;
        }
        return new PaymentEntity(
                UUID.randomUUID(),
                dto.amount(),
                dto.paidAmount() != null ? dto.paidAmount() : BigDecimal.ZERO,
                dto.dueDate(),
                dto.paidDate(),
                dto.paymentMethod() != null ? dto.paymentMethod() : PaymentMethod.NONE,
                dto.transactionId(),
                dto.notes()
        );
    }

    public PaymentResponseDTO fromEntity(PaymentEntity entity, List<UserPlanResponseDTO> plans) {
        if (entity == null) {
            return null;
        }
        return new PaymentResponseDTO(
                entity.getId(),
                entity.getAmount(),
                entity.getPaidAmount(),
                entity.getDueDate(),
                entity.getPaidDate(),
                entity.getPaymentMethod(),
                entity.getTransactionId(),
                entity.getNotes(),
                plans != null ? plans : List.of()
        );
    }
}

