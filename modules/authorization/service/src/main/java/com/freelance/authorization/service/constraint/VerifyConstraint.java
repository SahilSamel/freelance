package com.freelance.authorization.service.constraint;

import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.error.authorization.ConstraintErrors;
import com.freelance.identityManagement.mapping.userRole.UserRoleSearchRequestDTO;
import com.freelance.identityManagement.mapping.userRole.UserRoleSearchResponseDTO;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.userRole.UserRoleService;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.app.transaction.Transaction;
import java.util.Set;
import java.util.UUID;


public class VerifyConstraint extends AbstractConstraint {

    public void checkConstraints(){
        UserRoleSearchRequestDTO userRoleSearchRequestDTO = new UserRoleSearchRequestDTO(Transaction.getRequestContext().userId());
        UserRoleService userRoleService = new UserRoleService();
        UserRoleSearchResponseDTO rolesAssignedToUser = userRoleService.fetchAssignedRoles(userRoleSearchRequestDTO);
        Set<String> authorizedKeysForRole;
        for(UUID roleId : rolesAssignedToUser.roleIds()){
            authorizedKeysForRole = CacheManager.getInstance().get(RoleAuthorizationKeyCacheConfig.NAME, roleId.toString());
            if(authorizedKeysForRole.contains(Transaction.getRequestContext().authorizationKey())){
                return;
            }
        }
        raiseConstraintError(ConstraintErrors.UNAUTHORIZED);
    }

}
