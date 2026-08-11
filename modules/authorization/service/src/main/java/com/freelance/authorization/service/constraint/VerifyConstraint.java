package com.freelance.authorization.service.constraint;

import com.freelance.authorization.dto.role.RoleSearchRequestDTO;
import com.freelance.authorization.dto.role.RoleSearchResponseDTO;
import com.freelance.authorization.service.RoleService;
import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.error.authorization.ConstraintErrors;
import com.freelance.manager.CacheManager;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.app.transaction.Transaction;
import java.util.Set;
import java.util.UUID;


public class VerifyConstraint extends AbstractConstraint {

    public void checkConstraints(){
        RoleSearchRequestDTO roleSearchRequestDTO = new RoleSearchRequestDTO(Transaction.getRequestContext().userId());
        RoleService roleService = new RoleService();
        RoleSearchResponseDTO rolesAssignedToUser = roleService.fetchAssignedRoles(roleSearchRequestDTO);
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
