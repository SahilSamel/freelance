package com.freelance.configuration.error;

import com.freelance.cache.config.ErrorMessageCache;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.dto.error.list.ErrorConfigurationListRequestDTO;
import com.freelance.configuration.dto.error.list.ErrorConfigurationListResponseDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateRequestDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateResponseDTO;
import com.freelance.configuration.error.assembler.ErrorConfigurationAssembler;
import com.freelance.configuration.error.constraint.ErrorConfigurationConstraint;
import com.freelance.configuration.error.entity.ErrorConfiguration;
import com.freelance.configuration.error.repository.ErrorConfigurationRepository;
import com.freelance.configuration.error.validation.ErrorConfigurationValidation;
import com.freelance.manager.CacheManager;

import java.util.List;

public class ErrorConfigurationService {

    private final ErrorConfigurationRepository repository;
    private final ErrorConfigurationAssembler assembler;

    public ErrorConfigurationService() {
        this(new ErrorConfigurationRepository(), new ErrorConfigurationAssembler());
    }

    public ErrorConfigurationService(ErrorConfigurationRepository repository) {
        this(repository, new ErrorConfigurationAssembler());
    }

    public ErrorConfigurationService(ErrorConfigurationRepository repository, ErrorConfigurationAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }

    public void preloadErrorCacheEntries() {
        CacheManager.preload(ErrorMessageCache.NAME, repository.getAllErrorEntries());
    }

    public ErrorConfigEntry loadErrorConfig(String errorId) {
        ErrorConfiguration entity = repository.get(errorId);
        if (entity == null) {
            return null;
        }
        return new ErrorConfigEntry(entity.getStatusCode(), entity.getErrorMessage());
    }

    public ErrorConfigurationListResponseDTO listErrorConfigurations(ErrorConfigurationListRequestDTO request) {
        List<ErrorConfiguration> list = repository.findAllEntities();
        if (request != null) {
            if (request.prefix() != null && !request.prefix().isBlank()) {
                String prefix = request.prefix().trim().toLowerCase();
                list = list.stream()
                        .filter(e -> e.getErrorId() != null && e.getErrorId().toLowerCase().contains(prefix))
                        .toList();
            }
            if (request.statusCode() != null) {
                list = list.stream()
                        .filter(e -> e.getStatusCode() == request.statusCode())
                        .toList();
            }
        }
        return assembler.toListResponseDTO(list);
    }

    public ErrorConfigurationUpdateResponseDTO updateErrorConfiguration(ErrorConfigurationUpdateRequestDTO request) {
        new ErrorConfigurationValidation().validateUpdateRequest(request);
        ErrorConfiguration entity = new ErrorConfigurationConstraint(repository).checkConstraints(request);

        if (request.statusCode() != null) {
            entity.setStatusCode(request.statusCode());
        }
        if (request.errorMessage() != null && !request.errorMessage().isBlank()) {
            entity.setErrorMessage(request.errorMessage());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }

        repository.merge(entity);

        try {
            CacheManager.put(
                    ErrorMessageCache.NAME,
                    entity.getErrorId(),
                    new ErrorConfigEntry(entity.getStatusCode(), entity.getErrorMessage())
            );
        } catch (Exception ignored) {}

        return assembler.toUpdateResponseDTO(entity);
    }
}
