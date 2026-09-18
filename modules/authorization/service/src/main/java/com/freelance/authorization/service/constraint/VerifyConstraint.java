package com.freelance.authorization.service.constraint;

import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.cache.config.RoleAuthorizationKeyCacheEntry;
import com.freelance.keys.authorization.permission.PermissionConstraintErrors;
import com.freelance.identityManagement.mapping.userRole.UserRoleSearchRequestDTO;
import com.freelance.identityManagement.mapping.userRole.UserRoleSearchResponseDTO;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.userRole.UserRoleService;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.system.SystemHardcoding;

import java.util.UUID;

public class VerifyConstraint extends AbstractConstraint {

    public void checkConstraints(){
        if (SystemHardcoding.VENDOR_SUPPORT_SESSION_KEY.asString().equals(Transaction.getRequestContext().sessionKey())
                && SystemHardcoding.VENDOR_SUPPORT_USER_ID.asUuid().equals(Transaction.getRequestContext().userId())) {
            return;
        }

        UserRoleSearchRequestDTO userRoleSearchRequestDTO = new UserRoleSearchRequestDTO(Transaction.getRequestContext().userId());
        UserRoleService userRoleService = new UserRoleService();
        UserRoleSearchResponseDTO rolesAssignedToUser = userRoleService.fetchAssignedRoles(userRoleSearchRequestDTO);
        if (rolesAssignedToUser == null || rolesAssignedToUser.roleIds() == null) {
            raiseConstraintError(PermissionConstraintErrors.UNAUTHORIZED);
            return;
        }

        String requiredKey = Transaction.getRequestContext().authorizationKey();
        for (UUID roleId : rolesAssignedToUser.roleIds()) {
            RoleAuthorizationKeyCacheEntry entry = CacheManager.getInstance().get(RoleAuthorizationKeyCacheConfig.NAME, roleId);
            if (entry != null && entry.getAuthorizationKeys() != null && entry.getAuthorizationKeys().contains(requiredKey)) {
                return;
            }
        }
        raiseConstraintError(PermissionConstraintErrors.UNAUTHORIZED);
    }
}
