package com.freelance.user.delete;

import com.freelance.entity.user.User;
import com.freelance.repository.user.UserRepository;
import com.freelance.user.delete.constraint.UserDeleteConstraint;
import com.freelance.user.delete.validation.UserDeleteValidation;

public class UserDeleteService {

    private final UserRepository userRepository;

    public UserDeleteService() {
        this(new UserRepository());
    }

    public UserDeleteService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDeleteResponseDTO deleteUser(UserDeleteRequestDTO request) {
        new UserDeleteValidation().validateRequest(request);
        User existingUser = new UserDeleteConstraint(userRepository).checkConstraints(request);

        userRepository.remove(existingUser);

        return new UserDeleteResponseDTO(request.id(), true);
    }
}
