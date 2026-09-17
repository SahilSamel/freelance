package com.freelance.authorization.seeder;

import com.freelance.authorization.entity.Authorization;
import com.freelance.authorization.repository.AuthorizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Synchronizes code-defined {@link com.freelance.rest.app.endpoint.annotation.Authorization} annotations
 * into the database at application startup using the repository layer.
 * <p>
 * Performs full 2-way reconciliation:
 * <ul>
 *     <li>Inserts new permissions discovered from code.</li>
 *     <li>Updates changed metadata (names, descriptions, group paths).</li>
 *     <li>Deletes stale authorizations that were removed from code.</li>
 *     <li>Bypasses database writes entirely if no changes are detected.</li>
 * </ul>
 * </p>
 */
public class AuthorizationSeeder {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationSeeder.class);

    private static final int BATCH_SIZE = 100;
    private final AuthorizationRepository authorizationRepo;

    public AuthorizationSeeder() {
        this(new AuthorizationRepository());
    }

    public AuthorizationSeeder(AuthorizationRepository authorizationRepo) {
        this.authorizationRepo = authorizationRepo;
    }

    public void seed() {
        List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations =
                AuthorizationRegistry.getAllAuthorizations();

        if (annotations.isEmpty()) {
            logger.info("[AuthorizationSeeder] No @Authorization annotations discovered on classpath.");
            return;
        }

        syncAuthorizations(annotations);
        logger.info("[AuthorizationSeeder] Authorization database synchronization completed successfully.");
    }

    private void syncAuthorizations(
            List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations
    ) {
        Map<String, Authorization> existingAuthMap = authorizationRepo.findAllAsMap();
        Set<String> codeKeys = new HashSet<>();

        List<Authorization> toInsert = new ArrayList<>();
        List<Authorization> toUpdate = new ArrayList<>();

        for (com.freelance.rest.app.endpoint.annotation.Authorization authAnn : annotations) {
            String key = authAnn.key();
            codeKeys.add(key);
            Authorization existing = existingAuthMap.get(key);

            if (existing == null) {
                Authorization newAuth = new Authorization(
                        UUID.randomUUID(),
                        key,
                        authAnn.name(),
                        authAnn.description(),
                        authAnn.group()
                );
                toInsert.add(newAuth);
            } else {
                boolean changed = !Objects.equals(existing.getName(), authAnn.name())
                        || !Objects.equals(existing.getDescription(), authAnn.description())
                        || !Objects.equals(existing.getGroupPath(), authAnn.group());

                if (changed) {
                    existing.setName(authAnn.name());
                    existing.setDescription(authAnn.description());
                    existing.setGroupPath(authAnn.group());
                    toUpdate.add(existing);
                }
            }
        }

        List<Authorization> toDelete = new ArrayList<>();
        for (Map.Entry<String, Authorization> entry : existingAuthMap.entrySet()) {
            if (!codeKeys.contains(entry.getKey())) {
                toDelete.add(entry.getValue());
            }
        }

        if (!toInsert.isEmpty()) {
            authorizationRepo.persistEntriesInBatch(toInsert, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Inserted {} new authorization(s).", toInsert.size());
        }

        if (!toUpdate.isEmpty()) {
            authorizationRepo.mergeEntriesInBatch(toUpdate, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Updated metadata for {} authorization(s).", toUpdate.size());
        }

        if (!toDelete.isEmpty()) {
            authorizationRepo.removeEntriesInBatch(toDelete, BATCH_SIZE);
            logger.info("[AuthorizationSeeder] Removed {} stale authorization(s) no longer in code.", toDelete.size());
        }

        if (toInsert.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty()) {
            logger.debug("[AuthorizationSeeder] Authorizations are up to date. 0 DB modifications required.");
            return;
        }

        authorizationRepo.findAllAsMap();
    }
}
