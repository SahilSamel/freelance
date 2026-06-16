package User.Authentication;

import identityManagement.PhoneNumber;

public record UserAuthDetailsRequestDTO(
        String email,
        PhoneNumber phoneNumber,
        String username
) {
}
