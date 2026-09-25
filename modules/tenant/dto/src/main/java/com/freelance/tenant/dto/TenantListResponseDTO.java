package com.freelance.tenant.dto;

import java.util.List;

public record TenantListResponseDTO(
        List<TenantResponseDTO> tenants
) {}
