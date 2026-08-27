package com.freelance.rest.app.repository;

import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic base repository providing standardized CRUD and query operations
 * for JPA entities, backed by the current transaction's {@link EntityManager}.
 *
 * @param <T>  the entity type
 * @param <ID> the primary key identifier type
 */
public abstract class AbstractRepository<T, ID> {

    protected final Class<T> entityClass;

    @SuppressWarnings("unchecked")
    protected AbstractRepository() {
        Class<?> current = getClass();
        Type genericSuper = current.getGenericSuperclass();

        while (!(genericSuper instanceof ParameterizedType) && current.getSuperclass() != null) {
            current = current.getSuperclass();
            genericSuper = current.getGenericSuperclass();
        }

        if (genericSuper instanceof ParameterizedType pt && pt.getActualTypeArguments().length > 0) {
            this.entityClass = (Class<T>) pt.getActualTypeArguments()[0];
        } else {
            this.entityClass = null;
        }
    }

    /**
     * Retrieves the {@link EntityManager} bound to the current thread's
     * active transaction.
     */
    protected EntityManager em() {
        return Transaction.peek().getEntityManager();
    }

    public T get(ID id) {
        return id == null ? null : em().find(entityClass, id);
    }

    public void persist(T entity) {
        em().persist(entity);
    }

    public T merge(T entity) {
        return em().merge(entity);
    }

    public void remove(T entity) {
        if (entity != null) {
            EntityManager em = em();
            em.remove(em.contains(entity) ? entity : em.merge(entity));
        }
    }

    /**
     * Persists a collection of entities in batches, periodically flushing and clearing the persistence context.
     *
     * @param entities  the list of entity instances to persist
     * @param batchSize optional custom batch size for periodic flush and clear cycles (defaults to 50 if omitted)
     */
    public void persistEntriesInBatch(List<T> entities, int... batchSize) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        int effectiveBatchSize = (batchSize != null && batchSize.length > 0 && batchSize[0] > 0)
                ? batchSize[0]
                : 50;

        EntityManager em = em();
        for (int i = 0; i < entities.size(); i++) {
            em.persist(entities.get(i));
            if ((i + 1) % effectiveBatchSize == 0) {
                em.flush();
                em.clear();
            }
        }
        em.flush();
        em.clear();
    }

    /**
     * Merges a collection of entities in batches, periodically flushing and clearing the persistence context.
     *
     * @param entities  the list of entity instances to merge
     * @param batchSize optional custom batch size for periodic flush and clear cycles (defaults to 50 if omitted)
     * @return list of managed merged entity instances
     */
    public List<T> mergeEntriesInBatch(List<T> entities, int... batchSize) {
        if (entities == null || entities.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        int effectiveBatchSize = (batchSize != null && batchSize.length > 0 && batchSize[0] > 0)
                ? batchSize[0]
                : 50;

        EntityManager em = em();
        List<T> mergedList = new java.util.ArrayList<>(entities.size());

        for (int i = 0; i < entities.size(); i++) {
            T entity = entities.get(i);
            if (entity != null) {
                mergedList.add(em.merge(entity));
            }
            if ((i + 1) % effectiveBatchSize == 0) {
                em.flush();
                em.clear();
            }
        }
        em.flush();
        em.clear();
        return mergedList;
    }

    /**
     * Removes a collection of entities in batches, periodically flushing and clearing the persistence context.
     *
     * @param entities  the list of entity instances to remove
     * @param batchSize optional custom batch size for periodic flush and clear cycles (defaults to 50 if omitted)
     */
    public void removeEntriesInBatch(List<T> entities, int... batchSize) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        int effectiveBatchSize = (batchSize != null && batchSize.length > 0 && batchSize[0] > 0)
                ? batchSize[0]
                : 50;

        EntityManager em = em();
        for (int i = 0; i < entities.size(); i++) {
            T entity = entities.get(i);
            if (entity != null) {
                em.remove(em.contains(entity) ? entity : em.merge(entity));
            }
            if ((i + 1) % effectiveBatchSize == 0) {
                em.flush();
                em.clear();
            }
        }
        em.flush();
        em.clear();
    }

    /**
     * Executes a registered JPA {@link jakarta.persistence.NamedQuery} that expects at most a single result.
     * <p>
     * <b>Usage Guidelines:</b>
     * <ul>
     *   <li>{@code queryName} must be the exact identifier of a {@code @NamedQuery} declared on the entity class
     *       (e.g., {@code "User.findByUsername"}), <b>not</b> raw JPQL/SQL query strings.</li>
     *   <li>{@code params} binds named parameters defined in the query (e.g., a query containing {@code :username}
     *       is mapped with key {@code "username"}). Pass {@code null} or an empty map if no parameters are needed.</li>
     * </ul>
     * </p>
     *
     * @param queryName the exact registered {@code @NamedQuery} name (e.g., {@code "User.findByUsername"})
     * @param params    map of parameter names (without the leading colon) to parameter values; may be {@code null}
     * @return an {@link Optional} containing the matched entity, or {@link Optional#empty()} if no row was found
     * @throws jakarta.persistence.NonUniqueResultException if more than one matching entity is found
     * @throws IllegalArgumentException                    if no named query is registered under {@code queryName}
     */
    protected Optional<T> executeSingleResultSearch(String queryName, Map<String, Object> params) {
        try {
            TypedQuery<T> query = em().createNamedQuery(queryName, entityClass);
            if (params != null) {
                params.forEach(query::setParameter);
            }
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    protected List<T> executeListResultSearch(String queryName, Map<String, Object> params) {
        TypedQuery<T> query = em().createNamedQuery(queryName, entityClass);
        if (params != null) {
            params.forEach(query::setParameter);
        }
        return query.getResultList();
    }

    protected <R> List<R> executeTypedListQuery(String queryName, Class<R> resultClass, Map<String, Object> params) {
        TypedQuery<R> query = em().createNamedQuery(queryName, resultClass);
        if (params != null) {
            params.forEach(query::setParameter);
        }
        return query.getResultList();
    }

    protected int executeUpdateQuery(String queryName, Map<String, Object> params) {
        var query = em().createNamedQuery(queryName);
        if (params != null) {
            params.forEach(query::setParameter);
        }
        return query.executeUpdate();
    }
}