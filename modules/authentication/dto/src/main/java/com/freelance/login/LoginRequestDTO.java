package com.freelance.login;

import com.freelance.identityManagement.PhoneNumber;

public record LoginRequestDTO(
    String username,
    String email,
    PhoneNumber phoneNumber,
    String password
) {
}
