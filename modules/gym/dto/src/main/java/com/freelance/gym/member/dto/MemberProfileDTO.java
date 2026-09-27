package com.freelance.gym.member.dto;

import com.freelance.identityManagement.PhoneNumber;

import java.io.Serializable;

public record MemberProfileDTO(
        String firstName,
        String lastName,
        PhoneNumber phoneNumber
) implements Serializable {
}
