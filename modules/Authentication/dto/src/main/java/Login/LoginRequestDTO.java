package Login;

import IdentityManagement.PhoneNumber;

public record LoginRequestDTO(
    String username,
    String email,
    PhoneNumber phoneNumber,
    String password
) {
}
