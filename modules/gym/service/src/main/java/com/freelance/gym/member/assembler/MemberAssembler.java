package com.freelance.gym.member.assembler;

import com.freelance.gym.member.dto.MemberRecordDTO;
import com.freelance.gym.member.dto.UserPlanResponseDTO;
import com.freelance.gym.payment.dto.PaymentResponseDTO;
import com.freelance.identityManagement.PhoneNumber;
import com.freelance.user.UserDTO;

import java.util.List;

public class MemberAssembler {

    public MemberRecordDTO toMemberRecord(
            UserDTO user,
            PhoneNumber phoneNumber,
            String firstName,
            String lastName,
            List<UserPlanResponseDTO> plans,
            List<PaymentResponseDTO> payments
    ) {
        if (user == null) {
            return null;
        }
        return new MemberRecordDTO(
                user.id(),
                user.username(),
                firstName != null ? firstName : user.firstName(),
                lastName != null ? lastName : user.lastName(),
                phoneNumber,
                user.createdDate(),
                plans != null ? plans : List.of(),
                payments != null ? payments : List.of()
        );
    }
}


