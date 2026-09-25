package com.freelance.user.delete;

import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.mapping.userRole.repository.UserRoleRepository;
import com.freelance.user.entity.User;
import com.freelance.user.repository.UserRepository;
import com.freelance.user.delete.constraint.UserDeleteConstraint;
import com.freelance.user.delete.validation.UserDeleteValidation;

import java.util.List;

public class UserDeleteService {

    private final UserRepository userRepository;
    private final UserContactRepository userContactRepository;
    private final UserRoleRepository userRoleRepository;

    public UserDeleteService() {
        this(new UserRepository(), new UserContactRepository(), new UserRoleRepository());
    }

    public UserDeleteService(UserRepository userRepository) {
        this(userRepository, new UserContactRepository(), new UserRoleRepository());
    }

    public UserDeleteService(UserRepository userRepository, UserContactRepository userContactRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userContactRepository = userContactRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public UserDeleteResponseDTO deleteUser(UserDeleteRequestDTO request) {
        new UserDeleteValidation().validateRequest(request);
        User existingUser = new UserDeleteConstraint(userRepository).checkConstraints(request);

        if (request != null && request.id() != null) {
            List<UserContactEntity> contacts = userContactRepository.findByUserId(request.id());
            if (contacts != null) {
                for (UserContactEntity contact : contacts) {
                    userContactRepository.remove(contact);
                }
            }
            userRoleRepository.deleteByUserId(request.id());
        }

        userRepository.remove(existingUser);

        return new UserDeleteResponseDTO(request.id(), true);
    }
}
