package com.freelance.rest.app.service.repository;

import com.freelance.rest.app.transaction.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

/**
 * Generic base repository providing common CRUD and query operations
 * for a given entity type, backed by the active transaction's
 * {@link EntityManager}.
 *
 * @param <T>  the entity type
 * @param <ID> the entity's primary key type
 */
public abstract class AbstractRepository<T, ID> {

    /**
     * Runtime class of the entity, resolved via reflection for use with JPA.
     */
    protected final Class<T> entityClass;

    /**
     * Resolves the concrete entity type from the generic type parameter
     * of the subclass (e.g. {@code class UserRepository extends AbstractRepository<User, Long>}).
     */
    @SuppressWarnings("unchecked")
    protected AbstractRepository() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    /**
     * Retrieves the {@link EntityManager} bound to the current thread's
     * active transaction.
     */
    protected EntityManager em() {
        return Transaction.peek().getEntityManager();
    }

    /**
     * Finds an entity by its primary key.
     *
     * @param id the primary key; if null, no lookup is performed
     * @return the matching entity, or null if not found (or if id was null)
     */
    public T get(ID id) {
        return id == null ? null : em().find(entityClass, id);
    }

    /**
     * Persists a new entity into the active persistence context.
     */
    public void persist(T entity) {
        em().persist(entity);
    }

    /**
     * Merges changes made to a detached entity back into the current
     * persistence context.
     *
     * @return the managed entity instance reflecting the merge
     */
    public T merge(T entity) {
        return em().merge(entity);
    }

    /**
     * Deletes the given entity, merging it into the persistence context
     * first if it isn't already managed.
     */
    public void remove(T entity) {
        if (entity != null) {
            EntityManager em = em();
            em.remove(em.contains(entity) ? entity : em.merge(entity));
        }
    }

    /**
     * Executes a named query with the given parameters and returns all matches.
     *
     * @param queryName the named query identifier (e.g. "User.findAllActive")
     * @param params    parameter name/value pairs to bind, or null if none are needed
     * @return the matching entities, or an empty list if none are found
     */
    protected java.util.List<T> executeSearch(String queryName, Map<String, Object> params) {
        TypedQuery<T> query = em().createNamedQuery(queryName, entityClass);
        if (params != null) {
            params.forEach(query::setParameter);
        }
        return query.getResultList();
    }

}