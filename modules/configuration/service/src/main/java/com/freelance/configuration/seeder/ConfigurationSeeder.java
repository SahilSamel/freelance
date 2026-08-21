package com.freelance.configuration.seeder;

import com.freelance.config.IConfig;
import com.freelance.configuration.entity.error.ErrorConfiguration;
import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.configuration.repository.error.ErrorConfigurationRepository;
import com.freelance.configuration.repository.system.SystemConfigurationRepository;
import com.freelance.config.IError;
import com.freelance.system.SystemHardcoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Synchronizes code-defined IConfig and IError defaults into the database at boot.
 * Deletes all non-modified system-owned entries, then inserts code definitions if not already
 * preserved as an admin modification.
 */
public class ConfigurationSeeder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationSeeder.class);

    private static final int BATCH_SIZE = 100;
    private final SystemConfigurationRepository systemConfigRepo;
    private final ErrorConfigurationRepository errorConfigRepo;

    public ConfigurationSeeder() {
        this(new SystemConfigurationRepository(), new ErrorConfigurationRepository());
    }

    public ConfigurationSeeder(SystemConfigurationRepository systemConfigRepo, ErrorConfigurationRepository errorConfigRepo) {
        this.systemConfigRepo = systemConfigRepo;
        this.errorConfigRepo = errorConfigRepo;
    }

    public void seed() {
        UUID systemUserId = SystemHardcoding.SYSTEM_USER_ID.asUuid();

        seedSystemConfigurations();
        seedErrorConfigurations(systemUserId);
    }

    private void seedSystemConfigurations() {
        int deleted = systemConfigRepo.deleteSystemOwned();
        logger.debug("[ConfigurationSeeder] Cleared {} unmodified system config rows.", deleted);

        List<IConfig> configs = ConfigurationRegistry.getAllConfigs();
        Set<String> existingKeys = systemConfigRepo.findAllIds();

        List<SystemConfiguration> toInsert = configs.stream()
                .filter(c -> !existingKeys.contains(c.getKey()))
                .map(c -> new SystemConfiguration(
                        c.getKey(),
                        c.getDefaultValue(),
                        c.getDescription()
                ))
                .toList();

        systemConfigRepo.persistBatch(toInsert, BATCH_SIZE);
        logger.debug("[ConfigurationSeeder] Seeded {} system config rows from code.", toInsert.size());
    }

    private void seedErrorConfigurations(UUID systemUserId) {
        List<IError> errors = ConfigurationRegistry.getAllErrors();

        // Validate uniqueness of error indirection codes across the codebase
        Map<String, IError> seenCodes = new HashMap<>();
        for (IError error : errors) {
            String code = error.getIndirectionCode();
            if (code == null || code.isBlank()) {
                throw new IllegalStateException("FATAL: Error " + error + " is missing an indirection code!");
            }
            IError duplicate = seenCodes.put(code, error);
            if (duplicate != null) {
                throw new IllegalStateException(String.format(
                        "FATAL: Duplicate error indirection code '%s' detected between [%s] and [%s]!",
                        code, duplicate, error
                ));
            }
        }

        int deleted = errorConfigRepo.deleteSystemOwned(systemUserId);
        logger.debug("[ConfigurationSeeder] Cleared {} unmodified error message rows.", deleted);

        Set<String> existingKeys = errorConfigRepo.findAllIds();

        List<ErrorConfiguration> toInsert = errors.stream()
                .filter(e -> !existingKeys.contains(e.getErrorKey()))
                .map(e -> new ErrorConfiguration(
                        e.getErrorKey(),
                        e.getDefaultStatusCode(),
                        e.getDefaultMessage(),
                        e.getDescription()
                ))
                .toList();

        errorConfigRepo.persistBatch(toInsert, BATCH_SIZE);
        logger.debug("[ConfigurationSeeder] Seeded {} error message rows from code.", toInsert.size());
    }
}
