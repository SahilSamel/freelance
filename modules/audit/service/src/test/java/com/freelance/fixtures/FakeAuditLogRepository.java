package com.freelance.fixtures;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.audit.entity.AuditLog;
import com.freelance.audit.repository.AuditLogRepository;

import java.util.*;

public class FakeAuditLogRepository extends AuditLogRepository {
    public final List<AuditLog> storage = new ArrayList<>();

    @Override
    public void persist(AuditLog entity) {
        if (entity != null) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            storage.add(entity);
        }
    }

    @Override
    public List<AuditLog> findAuditLogs(AuditLogListRequestDTO filter) {
        List<AuditLog> result = new ArrayList<>(storage);
        if (filter != null) {
            if (filter.userId() != null) {
                result.removeIf(a -> !filter.userId().equals(a.getUserId()));
            }
            if (filter.traceId() != null && !filter.traceId().isBlank()) {
                result.removeIf(a -> !filter.traceId().trim().equals(a.getTraceId()));
            }
            if (filter.httpMethod() != null && !filter.httpMethod().isBlank()) {
                result.removeIf(a -> !filter.httpMethod().trim().equalsIgnoreCase(a.getHttpMethod()));
            }
            if (filter.endpoint() != null && !filter.endpoint().isBlank()) {
                result.removeIf(a -> a.getEndpoint() == null || !a.getEndpoint().contains(filter.endpoint().trim()));
            }
            if (filter.statusCode() != null) {
                result.removeIf(a -> a.getStatusCode() != filter.statusCode());
            }
            if (filter.fromTimestamp() != null) {
                result.removeIf(a -> a.getTimestamp().isBefore(filter.fromTimestamp()));
            }
            if (filter.toTimestamp() != null) {
                result.removeIf(a -> a.getTimestamp().isAfter(filter.toTimestamp()));
            }
        }
        return result;
    }

    @Override
    public List<AuditLog> findAll() {
        return new ArrayList<>(storage);
    }
}
