package com.freelance.user.create;

import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.mapping.userRole.entity.UserRoleEntity;
import com.freelance.mapping.userRole.repository.UserRoleRepository;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.contact.UserContactResponseDTO;
import com.freelance.user.entity.User;
import com.freelance.user.repository.UserRepository;
import com.freelance.user.create.assembler.UserCreateAssembler;
import com.freelance.user.create.constraint.UserCreateConstraint;
import com.freelance.user.create.validation.UserCreateValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserCreateService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserContactRepository userContactRepository;
    private final UserCreateAssembler assembler;

    public UserCreateService() {
        this(new UserRepository(), new UserRoleRepository(), new UserContactRepository(), new UserCreateAssembler());
    }

    public UserCreateService(UserRepository userRepository, UserCreateAssembler assembler) {
        this(userRepository, new UserRoleRepository(), new UserContactRepository(), assembler);
    }

    public UserCreateService(UserRepository userRepository, UserRoleRepository userRoleRepository, UserCreateAssembler assembler) {
        this(userRepository, userRoleRepository, new UserContactRepository(), assembler);
    }

    public UserCreateService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            UserContactRepository userContactRepository,
            UserCreateAssembler assembler
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userContactRepository = userContactRepository;
        this.assembler = assembler;
    }

    public UserCreateResponseDTO createUser(UserCreateRequestDTO request) {
        new UserCreateValidation().validateRequest(request);
        new UserCreateConstraint(userRepository, userContactRepository).checkConstraints(request);

        User user = assembler.toEntity(request);
        userRepository.persist(user);

        List<UUID> assignedRoleIds = new ArrayList<>();
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            for (UUID roleId : request.roleIds()) {
                if (roleId != null) {
                    UserRoleEntity userRole = new UserRoleEntity(user.getId(), roleId);
                    userRoleRepository.persist(userRole);
                    assignedRoleIds.add(roleId);
                }
            }
        }

        return assembler.fromEntity(user, assignedRoleIds);
    }
}
