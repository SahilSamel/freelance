package com.freelance.configuration.seeder;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import com.freelance.registry.ConfigurationRegistry;
import com.freelance.configuration.error.entity.ErrorConfiguration;
import com.freelance.configuration.system.entity.SystemConfiguration;
import com.freelance.configuration.error.repository.ErrorConfigurationRepository;
import com.freelance.configuration.system.repository.SystemConfigurationRepository;
import com.freelance.system.SystemHardcoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Synchronizes code-defined {@link IConfig} and {@link IError} default constants into the database at application startup.
 * <p>
 * Performs full 2-way reconciliation:
 * <ul>
 *     <li>Inserts new system configurations and error messages.</li>
 *     <li>Updates changed descriptions/metadata and default values for system-owned rows while preserving administrator custom values.</li>
 *     <li>Deletes stale system-owned configurations and errors removed from code enums.</li>
 *     <li>Bypasses database writes entirely if no changes are detected.</li>
 * </ul>
 * </p>
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
        syncSystemConfigurations();
        syncErrorConfigurations();

        logger.info("[ConfigurationSeeder] Configuration database synchronization completed successfully.");
    }

    private void syncSystemConfigurations() {
        List<IConfig> configs = ConfigurationRegistry.getAllConfigs();
        Map<String, SystemConfiguration> existingMap = systemConfigRepo.findAllEntitiesAsMap();
        Set<String> codeKeys = new HashSet<>();

        List<SystemConfiguration> toInsert = new ArrayList<>();
        List<SystemConfiguration> toUpdate = new ArrayList<>();

        for (IConfig config : configs) {
            String key = config.getKey();
            codeKeys.add(key);
            SystemConfiguration existing = existingMap.get(key);

            if (existing == null) {
                toInsert.add(new SystemConfiguration(
                        key,
                        config.getDefaultValue(),
                        config.getDescription(),
                        config.getValidation()
                ));
            } else {
                boolean notModifiedByAnyUser = (existing.getLastUpdatedBy() == null || existing.getLastUpdatedBy().equals(SystemHardcoding.SYSTEM_USER_ID.asUuid()));
                boolean changed = false;

                if (notModifiedByAnyUser) {
                    if (!Objects.equals(existing.getConfigValue(), config.getDefaultValue())) {
                        existing.setConfigValue(config.getDefaultValue());
                        changed = true;
                    }
                }

                if (!Objects.equals(existing.getDescription(), config.getDescription())) {
                    existing.setDescription(config.getDescription());
                    changed = true;
                }

                if (!Objects.equals(existing.getValidation(), config.getValidation())) {
                    existing.setValidation(config.getValidation());
                    changed = true;

                    String pattern = config.getValidation();
                    if (pattern != null && !pattern.isBlank()) {
                        if (existing.getConfigValue() == null || !existing.getConfigValue().matches(pattern)) {
                            logger.warn("[ConfigurationSeeder] Current value '{}' of configuration '{}' violates updated validation pattern '{}'. Reverting to default value '{}'.",
                                    existing.getConfigValue(), key, pattern, config.getDefaultValue());
                            existing.setConfigValue(config.getDefaultValue());
                        }
                    }
                }

                if (changed) {
                    toUpdate.add(existing);
                }
            }
        }

        List<SystemConfiguration> toDelete = new ArrayList<>();
        for (Map.Entry<String, SystemConfiguration> entry : existingMap.entrySet()) {
            if (!codeKeys.contains(entry.getKey())) {
                SystemConfiguration entity = entry.getValue();
                boolean notModifiedByAnyUser = (entity.getLastUpdatedBy() == null || entity.getLastUpdatedBy().equals(SystemHardcoding.SYSTEM_USER_ID.asUuid()));
                if (notModifiedByAnyUser) {
                    toDelete.add(entity);
                }
            }
        }

        if (!toInsert.isEmpty()) {
            systemConfigRepo.persistEntriesInBatch(toInsert, BATCH_SIZE);
            logger.info("[ConfigurationSeeder] Inserted {} new system config row(s).", toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            systemConfigRepo.mergeEntriesInBatch(toUpdate, BATCH_SIZE);
            logger.info("[ConfigurationSeeder] Updated {} system config row(s).", toUpdate.size());
        }

        if (!toDelete.isEmpty()) {
            systemConfigRepo.removeEntriesInBatch(toDelete, BATCH_SIZE);
            logger.info("[ConfigurationSeeder] Removed {} stale system config row(s) no longer in code.", toDelete.size());
        }

        if (toInsert.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty()) {
            logger.debug("[ConfigurationSeeder] System configurations are up to date. 0 DB modifications required.");
        }
    }

    private void syncErrorConfigurations() {
        List<IError> errors = ConfigurationRegistry.getAllErrors();

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

        Map<String, ErrorConfiguration> existingMap = errorConfigRepo.findAllEntitiesAsMap();
        Set<String> codeKeys = new HashSet<>();

        List<ErrorConfiguration> toInsert = new ArrayList<>();
        List<ErrorConfiguration> toUpdate = new ArrayList<>();

        for (IError error : errors) {
            String key = error.getErrorKey();
            codeKeys.add(key);
            ErrorConfiguration existing = existingMap.get(key);

            if (existing == null) {
                toInsert.add(new ErrorConfiguration(
                        key,
                        error.getDefaultStatusCode(),
                        error.getDefaultMessage(),
                        error.getDescription()
                ));
            } else {
                boolean notModifiedByAnyUser = (existing.getLastUpdatedBy() == null || existing.getLastUpdatedBy().equals(SystemHardcoding.SYSTEM_USER_ID.asUuid()));
                boolean changed = false;

                if (notModifiedByAnyUser) {
                    if (existing.getStatusCode() != error.getDefaultStatusCode()) {
                        existing.setStatusCode(error.getDefaultStatusCode());
                        changed = true;
                    }
                    if (!Objects.equals(existing.getErrorMessage(), error.getDefaultMessage())) {
                        existing.setErrorMessage(error.getDefaultMessage());
                        changed = true;
                    }
                }

                if (!Objects.equals(existing.getDescription(), error.getDescription())) {
                    existing.setDescription(error.getDescription());
                    changed = true;
                }

                if (changed) {
                    toUpdate.add(existing);
                }
            }
        }

        List<ErrorConfiguration> toDelete = new ArrayList<>();
        for (Map.Entry<String, ErrorConfiguration> entry : existingMap.entrySet()) {
            if (!codeKeys.contains(entry.getKey())) {
                ErrorConfiguration entity = entry.getValue();
                boolean notModifiedByAnyUser = (entity.getLastUpdatedBy() == null || entity.getLastUpdatedBy().equals(SystemHardcoding.SYSTEM_USER_ID.asUuid()));
                if (notModifiedByAnyUser) {
                    toDelete.add(entity);
                }
            }
        }

        if (!toInsert.isEmpty()) {
            errorConfigRepo.persistEntriesInBatch(toInsert, BATCH_SIZE);
            logger.info("[ConfigurationSeeder] Inserted {} new error configuration row(s).", toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            errorConfigRepo.mergeEntriesInBatch(toUpdate, BATCH_SIZE);
            logger.info("[ConfigurationSeeder] Updated {} error configuration row(s).", toUpdate.size());
        }

        if (!toDelete.isEmpty()) {
            errorConfigRepo.removeEntriesInBatch(toDelete, BATCH_SIZE);
            logger.info("[ConfigurationSeeder] Removed {} stale error configuration row(s) no longer in code.", toDelete.size());
        }

        if (toInsert.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty()) {
            logger.debug("[ConfigurationSeeder] Error configurations are up to date. 0 DB modifications required.");
        }
    }
}
