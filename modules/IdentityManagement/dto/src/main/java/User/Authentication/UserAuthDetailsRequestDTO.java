package User.Authentication;

import IdentityManagement.PhoneNumber;

public record UserAuthDetailsRequestDTO(
        String email,
        PhoneNumber phoneNumber,
        String username
) {
}
