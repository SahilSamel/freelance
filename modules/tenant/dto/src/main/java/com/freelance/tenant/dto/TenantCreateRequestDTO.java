package com.freelance.tenant.dto;

public record TenantCreateRequestDTO(
        String tenantId,
        String name,
        String publicKey
) {
    public TenantCreateRequestDTO(String tenantId, String name) {
        this(tenantId, name, null);
    }
}
