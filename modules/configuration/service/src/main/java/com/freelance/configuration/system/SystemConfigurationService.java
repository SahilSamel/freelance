package com.freelance.configuration.system;

import com.freelance.cache.config.SystemConfigurationCache;
import com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListResponseDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateResponseDTO;
import com.freelance.configuration.system.entity.SystemConfiguration;
import com.freelance.configuration.system.repository.SystemConfigurationRepository;
import com.freelance.configuration.system.assembler.SystemConfigurationAssembler;
import com.freelance.configuration.system.constraint.SystemConfigurationConstraint;
import com.freelance.configuration.system.validation.SystemConfigurationValidation;
import com.freelance.manager.CacheManager;

import java.util.List;

/**
 * Service managing database lookups and cache preloading for dynamic application configuration properties.
 */
public class SystemConfigurationService {

    private final SystemConfigurationRepository repository;
    private final SystemConfigurationAssembler assembler;

    public SystemConfigurationService() {
        this(new SystemConfigurationRepository(), new SystemConfigurationAssembler());
    }

    public SystemConfigurationService(SystemConfigurationRepository repository) {
        this(repository, new SystemConfigurationAssembler());
    }

    public SystemConfigurationService(SystemConfigurationRepository repository, SystemConfigurationAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }

    public void preloadSystemCacheEntries() {
        CacheManager.preload(SystemConfigurationCache.NAME, repository.getAll());
    }

    public String loadConfigValue(String configId) {
        SystemConfiguration entity = repository.get(configId);
        return entity != null ? entity.getConfigValue() : null;
    }

    public SystemConfigurationListResponseDTO listConfigurations(SystemConfigurationListRequestDTO request) {
        List<SystemConfiguration> list = repository.findAllEntities();
        if (request != null && request.prefix() != null && !request.prefix().isBlank()) {
            String prefix = request.prefix().trim().toLowerCase();
            list = list.stream()
                    .filter(c -> c.getConfigId() != null && c.getConfigId().toLowerCase().contains(prefix))
                    .toList();
        }
        return assembler.toListResponseDTO(list);
    }

    public SystemConfigurationUpdateResponseDTO updateConfiguration(SystemConfigurationUpdateRequestDTO request) {
        new SystemConfigurationValidation().validateUpdateRequest(request);
        SystemConfiguration entity = new SystemConfigurationConstraint(repository).checkConstraints(request);

        entity.setConfigValue(request.configValue());
        if (request.description() != null) {
            entity.setDescription(request.description());
        }

        repository.merge(entity);

        try {
            CacheManager.put(SystemConfigurationCache.NAME, entity.getConfigId(), entity.getConfigValue());
        } catch (Exception ignored) {}

        return assembler.toUpdateResponseDTO(entity);
    }
}
