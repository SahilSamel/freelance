package com.freelance.auth.service;

import com.freelance.auth.dto.*;
import com.freelance.auth.service.constraint.LoginConstraint;
import com.freelance.auth.service.constraint.MeConstraint;
import com.freelance.auth.service.constraint.VerifyAuthConstraint;
import com.freelance.auth.service.validation.LoginValidation;
import com.freelance.auth.service.validation.VerifyAuthValidation;
import com.freelance.auth.session.Session;
import com.freelance.auth.token.TokenUtility;
import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.cache.config.RoleAuthorizationKeyCacheEntry;
import com.freelance.identityManagement.mapping.userRole.UserRoleSearchRequestDTO;
import com.freelance.identityManagement.mapping.userRole.UserRoleSearchResponseDTO;
import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.userRole.UserRoleService;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.role.entity.Role;
import com.freelance.role.repository.RoleRepository;
import com.freelance.system.SystemHardcoding;
import com.freelance.user.read.User;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;

import java.util.*;

public class Authentication {

    private final Session session = new Session();
    private final TokenUtility tokenUtility = new TokenUtility();

    public AuthLoginResponseDTO login(AuthLoginRequestDTO request) {
        new LoginValidation().validateRequest(request);
        new LoginConstraint().checkConstraints(request);

        User userService = new User();
        UserReadRequestDTO userReadRequestDTO = new UserReadRequestDTO(null, request.username());
        UserReadResponseDTO userReadResponseDTO = userService.findByUsername(userReadRequestDTO);

        SessionGenerateResponseDTO session = this.session.generateSession(new SessionGenerateRequestDTO(userReadResponseDTO.userDTO().id()));
        TokenGenerateResponseDTO token = tokenUtility.generateToken(new TokenGenerateRequestDTO(userReadResponseDTO.userDTO().id(), session.sessionKey(), Transaction.getRequestContext().tenantId()));

        return new AuthLoginResponseDTO(token.token());
    }

    public void logout() {
        session.invalidateSession(new SessionInvalidateRequestDTO(Transaction.getRequestContext().sessionKey(), Transaction.getRequestContext().userId()));
    }

    public AuthVerifyResponseDTO verify(AuthVerifyRequestDTO request) {
        new VerifyAuthValidation().validateRequest(request);
        new VerifyAuthConstraint().checkConstraints(request);

        if (tokenUtility.isVendorToken(request.token())) {
            return new AuthVerifyResponseDTO(
                    SystemHardcoding.VENDOR_SUPPORT_USER_ID.asUuid(),
                    SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString()
            );
        }

        TokenParseResponseDTO parsed = tokenUtility.parse(new TokenParseRequestDTO(request.token()));
        session.verifySession(new SessionVerifyRequestDTO(parsed.sessionKey(), parsed.userId()));

        return new AuthVerifyResponseDTO(parsed.userId(), parsed.sessionKey());
    }

    public AuthRefreshResponseDTO refreshSession() {
        String oldSessionKey = Transaction.getRequestContext().sessionKey();
        java.util.UUID userId = Transaction.getRequestContext().userId();

        SessionRefreshResponseDTO refreshed = session.refreshSession(new SessionRefreshRequestDTO(oldSessionKey, userId));

        if (refreshed.newSessionKey() == null) {
            throw new ConstraintException(SessionConstraintErrors.INVALID_SESSION);
        }
        TokenGenerateResponseDTO token = tokenUtility.generateToken(new TokenGenerateRequestDTO(userId, refreshed.newSessionKey(), Transaction.getRequestContext().tenantId()));
        return new AuthRefreshResponseDTO(token.token());
    }

    public AuthMeResponseDTO me() {
        new MeConstraint().checkConstraints();

        UUID userId = Transaction.getRequestContext().userId();
        User userService = new User();
        com.freelance.user.UserDTO userDTO = userService.findById(userId).orElseThrow();

        UserRoleService userRoleService = new UserRoleService();
        UserRoleSearchResponseDTO rolesRes = userRoleService.fetchAssignedRoles(new UserRoleSearchRequestDTO(userId));
        List<UUID> roleIds = rolesRes != null && rolesRes.roleIds() != null ? rolesRes.roleIds() : List.of();

        RoleRepository roleRepository = new RoleRepository();
        List<String> roleNames = new ArrayList<>();
        Set<String> allPermissions = new HashSet<>();

        for (UUID roleId : roleIds) {
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            roleOpt.ifPresent(r -> roleNames.add(r.getName()));

            RoleAuthorizationKeyCacheEntry entry = CacheManager.get(RoleAuthorizationKeyCacheConfig.NAME, roleId);
            if (entry != null && entry.getAuthorizationKeys() != null) {
                allPermissions.addAll(entry.getAuthorizationKeys());
            }
        }

        return new AuthMeResponseDTO(
                userDTO.id(),
                userDTO.username(),
                userDTO.firstName(),
                userDTO.lastName(),
                roleNames,
                new ArrayList<>(allPermissions)
        );
    }
}
