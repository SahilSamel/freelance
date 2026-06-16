package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import transaction.Transaction;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

public abstract class AbstractRepository<T, ID> {

    protected final Class<T> entityClass;

    @SuppressWarnings("unchecked")
    protected AbstractRepository() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    /**
     * Direct bridge fetching the running thread's active EntityManager
     * out of the Transaction component.
     */
    protected EntityManager em() {
        return Transaction.peek().getEntityManager();
    }

    /**
     * READ: Find a database row by its primary key ID.
     */
    public T get(ID id) {
        return id == null ? null : em().find(entityClass, id);
    }

    /**
     * CREATE: Place a fresh object into the active persistence context.
     */
    public void persist(T entity) {
        em().persist(entity);
    }

    /**
     * UPDATE: Merge updates made to an object back into the current context.
     */
    public T merge(T entity) {
        return em().merge(entity);
    }

    /**
     * DELETE: Remove the target entity completely from the database.
     */
    public void remove(T entity) {
        if (entity != null) {
            EntityManager em = em();
            em.remove(em.contains(entity) ? entity : em.merge(entity));
        }
    }

    /**
     * QUERY: Executes a declared NamedQuery utilizing a map of parameters and returns all matching records.
     * @param queryName The full descriptor matching the Entity annotation (e.g., "User.findAllActive")
     * @param params Key-Value pairs matching the placeholder variables inside the query (:status)
     * @return A List of matching entity records, or an empty list if no records are found.
     */
    protected java.util.List<T> executeSearch(String queryName, Map<String, Object> params) {
        TypedQuery<T> query = em().createNamedQuery(queryName, entityClass);
        if (params != null) {
            params.forEach(query::setParameter);
        }
        return query.getResultList();
    }


}