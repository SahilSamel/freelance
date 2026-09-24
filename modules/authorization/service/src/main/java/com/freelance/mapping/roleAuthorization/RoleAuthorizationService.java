package com.freelance.mapping.roleAuthorization;

import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.cache.config.RoleAuthorizationKeyCacheEntry;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.mapping.roleAuthorization.repository.RoleAuthorizationRepository;
import com.freelance.mapping.roleAuthorization.assembler.RoleAuthorizationAssembler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RoleAuthorizationService {

    private final RoleAuthorizationRepository roleAuthorizationRepository;


    public RoleAuthorizationService() {
        this.roleAuthorizationRepository = new RoleAuthorizationRepository();
    }

    public void preloadRoleAuthorizationCacheEntries() {
        Map<UUID, List<RoleAuthorization>> roleAuthorizationMappings =
                roleAuthorizationRepository.getAllMappingsGroupedByRoleId();

        RoleAuthorizationAssembler roleAuthorizationAssembler = new RoleAuthorizationAssembler();

        Map<UUID, RoleAuthorizationKeyCacheEntry> cacheMap =
                roleAuthorizationAssembler.toRoleIdToCacheEntryMap(roleAuthorizationMappings);

        CacheManager.preload(RoleAuthorizationKeyCacheConfig.NAME, cacheMap);
    }

    public RoleAuthorizationKeyCacheEntry loadRoleAuthorizationCacheEntryByRoleId(UUID roleId) {
        if (roleId == null) {
            return new RoleAuthorizationKeyCacheEntry();
        }

        List<RoleAuthorization> roleAuthorizations =
                roleAuthorizationRepository.getRoleAuthorizationsByRoleId(roleId);

        RoleAuthorizationAssembler roleAuthorizationAssembler = new RoleAuthorizationAssembler();

        return roleAuthorizationAssembler.toRoleAuthorizationCacheEntry(roleAuthorizations);
    }
}
