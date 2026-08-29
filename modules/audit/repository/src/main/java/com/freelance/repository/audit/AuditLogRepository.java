package com.freelance.repository.audit;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.entity.audit.AuditLog;
import com.freelance.rest.app.repository.AbstractRepository;
import jakarta.persistence.TypedQuery;

import java.util.*;

public class AuditLogRepository extends AbstractRepository<AuditLog, UUID> {

    public AuditLogRepository() {
        super();
    }

    public List<AuditLog> findAuditLogs(AuditLogListRequestDTO filter) {
        StringBuilder jpql = new StringBuilder("SELECT a FROM AuditLog a WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (filter != null) {
            if (filter.userId() != null) {
                jpql.append(" AND a.userId = :userId");
                params.put("userId", filter.userId());
            }
            if (filter.traceId() != null && !filter.traceId().isBlank()) {
                jpql.append(" AND a.traceId = :traceId");
                params.put("traceId", filter.traceId().trim());
            }
            if (filter.httpMethod() != null && !filter.httpMethod().isBlank()) {
                jpql.append(" AND a.httpMethod = :httpMethod");
                params.put("httpMethod", filter.httpMethod().trim().toUpperCase());
            }
            if (filter.endpoint() != null && !filter.endpoint().isBlank()) {
                jpql.append(" AND a.endpoint LIKE :endpoint");
                params.put("endpoint", "%" + filter.endpoint().trim() + "%");
            }
            if (filter.statusCode() != null) {
                jpql.append(" AND a.statusCode = :statusCode");
                params.put("statusCode", filter.statusCode());
            }
            if (filter.fromTimestamp() != null) {
                jpql.append(" AND a.timestamp >= :fromTimestamp");
                params.put("fromTimestamp", filter.fromTimestamp());
            }
            if (filter.toTimestamp() != null) {
                jpql.append(" AND a.timestamp <= :toTimestamp");
                params.put("toTimestamp", filter.toTimestamp());
            }
        }

        jpql.append(" ORDER BY a.timestamp DESC");

        TypedQuery<AuditLog> query = em().createQuery(jpql.toString(), AuditLog.class);
        params.forEach(query::setParameter);

        int limit = (filter != null && filter.limit() != null && filter.limit() > 0) ? filter.limit() : 50;
        int offset = (filter != null && filter.offset() != null && filter.offset() >= 0) ? filter.offset() : 0;

        query.setMaxResults(limit);
        query.setFirstResult(offset);

        return query.getResultList();
    }

    public List<AuditLog> findAll() {
        return findAuditLogs(null);
    }
}
