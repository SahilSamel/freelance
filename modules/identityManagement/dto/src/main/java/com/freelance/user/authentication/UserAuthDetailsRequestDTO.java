package com.freelance.user.authentication;

import com.freelance.identityManagement.PhoneNumber;

public record UserAuthDetailsRequestDTO(
        String email,
        PhoneNumber phoneNumber,
        String username
) {
}
