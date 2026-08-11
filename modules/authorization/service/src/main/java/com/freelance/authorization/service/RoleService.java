package com.freelance.authorization.service;

import com.freelance.authorization.dto.role.RoleSearchRequestDTO;
import com.freelance.authorization.dto.role.RoleSearchResponseDTO;
import com.freelance.authorization.service.assembler.RoleAuthorizationAssembler;
import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.cache.config.RoleAuthorizationKeyCacheEntry;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.roleAuth.entity.RoleAuthorization;
import com.freelance.mapping.roleAuth.repository.RoleAuthorizationRepository;
import com.freelance.mapping.roleUser.repository.RoleUserRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RoleService {

    private final RoleAuthorizationRepository roleAuthorizationRepository;
    private final RoleUserRepository roleUserRepository;

    public RoleService() {
        this.roleUserRepository = new RoleUserRepository();
        this.roleAuthorizationRepository = new RoleAuthorizationRepository();
    }

    public RoleSearchResponseDTO fetchAssignedRoles(RoleSearchRequestDTO roleSearchRequestDTO) {
        List<UUID> roleIds = roleUserRepository.findRoleIdsByUserId(roleSearchRequestDTO.userId());
        return new RoleSearchResponseDTO(roleIds);
    }

    public void initializeRoleAuthorizationCache(){
        Map<UUID, List<RoleAuthorization>> roleAuthorizationMappings = roleAuthorizationRepository.getAllMappingsGroupedByRoleId();
        RoleAuthorizationAssembler roleAuthorizationAssembler = new RoleAuthorizationAssembler();
        Map<UUID, Set<UUID>> roleAuthorizationEntries = roleAuthorizationAssembler.toRoleIdToAuthIdsMap(roleAuthorizationMappings);

        for(UUID role : roleAuthorizationEntries.keySet()){
            CacheManager.getInstance().put(RoleAuthorizationKeyCacheConfig.NAME, role.toString(), new RoleAuthorizationKeyCacheEntry(roleAuthorizationEntries.get(role)));
        }
    }
}
