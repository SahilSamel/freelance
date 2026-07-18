package com.freelance.provider.impl;

import com.freelance.config.data.CacheDefinition;
import com.freelance.provider.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Database-backed cache. Values are serialized to bytes and persisted
 * in the shared cache_store table, surviving application restarts.
 * Intended for values expensive to (re)compute (e.g. built from several
 * DB calls), so a single stored BLOB read replaces that whole composition.
 */
public final class DbCache implements Cache {

    private static final String CLASS_NAME = DbCache.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(DbCache.class);

    private final CacheDefinition definition;
    private final EntityManagerFactory emf;

    public DbCache(CacheDefinition definition, EntityManagerFactory emf) {
        this.definition = definition;
        this.emf = emf;
    }

    @Override
    public Object get(String key, Supplier<Object> loader) {
        try (EntityManager em = emf.createEntityManager()) {
            Object[] row = findRow(em, key);

            if (row != null) {
                Timestamp expiresAt = (Timestamp) row[1];
                boolean expired = expiresAt != null && expiresAt.toInstant().isBefore(Instant.now());

                if (!expired) {
                    try {
                        return deserialize((byte[]) row[0]);
                    } catch (Exception e) {
                        logger.debug("[{}] Failed to deserialize cached value for '{}':'{}'. Recomputing.",
                                CLASS_NAME, definition.name(), key, e);
                    }
                }
            }
        }

        Object value = loader.get();
        put(key, value);
        return value;
    }

    private Object[] findRow(EntityManager em, String key) {
        List<Object[]> results = em.createNativeQuery(
                        "SELECT value_blob, expires_at FROM cache_store WHERE cache_name = ?1 AND cache_key = ?2")
                .setParameter(1, definition.name())
                .setParameter(2, key)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void put(String key, Object value) {
        byte[] valueBlob = serialize(value);

        Timestamp expiresAt = definition.hasExpiry()
                ? Timestamp.from(Instant.now().plusSeconds(definition.ttlSeconds()))
                : null;

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery(
                            "INSERT INTO cache_store (cache_name, cache_key, value_blob, expires_at) " +
                                    "VALUES (?1, ?2, ?3, ?4) " +
                                    "ON CONFLICT (cache_name, cache_key) DO UPDATE SET value_blob = ?3, expires_at = ?4")
                    .setParameter(1, definition.name())
                    .setParameter(2, key)
                    .setParameter(3, valueBlob)
                    .setParameter(4, expiresAt)
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Override
    public void invalidate(String key) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("DELETE FROM cache_store WHERE cache_name = ?1 AND cache_key = ?2")
                    .setParameter(1, definition.name())
                    .setParameter(2, key)
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Override
    public void clear() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("DELETE FROM cache_store WHERE cache_name = ?1")
                    .setParameter(1, definition.name())
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    private static byte[] serialize(Object value) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize value for db-based cache store", e);
        }
    }

    private static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }
}
