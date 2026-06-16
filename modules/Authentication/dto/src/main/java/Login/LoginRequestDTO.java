package Login;

import identityManagement.PhoneNumber;

public record LoginRequestDTO(
    String username,
    String email,
    PhoneNumber phoneNumber,
    String password
) {
}
